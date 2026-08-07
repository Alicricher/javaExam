package com.dentistrybot.shared.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Оценивает ситуационные задачи через OpenAI Responses API с file_search
 * по ОБОИМ vector store (foreign+local) в одном запросе — модель сама
 * взвешивает международный и местный источники и выдаёт одну итоговую
 * оценку с аргументацией с обеих точек зрения (см. КИТОБЛАР/plan.md).
 * Если ответ студента совпадает хотя бы с одним источником — он не
 * считается неверным, просто не 100 баллов, с пометкой чего не хватает.
 */
public class GradingService {

    private static final Logger log = LoggerFactory.getLogger(GradingService.class);

    private static final String GRADING_SCHEMA_JSON = """
        {
          "type": "object",
          "additionalProperties": false,
          "properties": {
            "grade": { "type": "integer", "minimum": 0, "maximum": 100 },
            "criteria": {
              "type": "array",
              "items": {
                "type": "object",
                "additionalProperties": false,
                "properties": {
                  "name": { "type": "string" },
                  "points": { "type": "integer" },
                  "comment": { "type": "string" }
                },
                "required": ["name", "points", "comment"]
              }
            },
            "citations": { "type": "array", "items": { "type": "string" } },
            "confidence": { "type": "string", "enum": ["high", "medium", "low"] },
            "feedback": { "type": "string" },
            "passed": { "type": "boolean" },
            "source_gap": { "type": "boolean" }
          },
          "required": ["grade", "criteria", "citations", "confidence", "feedback", "passed", "source_gap"]
        }
        """;

    private final String apiKey;
    private final String model;
    private final String systemPrompt;
    private final String vectorStoreForeign;
    private final String vectorStoreLocal;
    private final Map<Integer, String> lessonPrompts = new HashMap<>();
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final Map<String, Object> gradingSchema;

    public GradingService(String baseUrl, String apiKey, String model, String systemPrompt,
                           String vectorStoreForeign, String vectorStoreLocal, ObjectMapper objectMapper) {
        this.apiKey = apiKey;
        this.model = model;
        this.systemPrompt = systemPrompt;
        this.vectorStoreForeign = vectorStoreForeign;
        this.vectorStoreLocal = vectorStoreLocal;
        this.objectMapper = objectMapper;
        this.webClient = WebClient.builder()
            .baseUrl(baseUrl)
            .defaultHeader("Authorization", "Bearer " + apiKey)
            .defaultHeader("Content-Type", "application/json")
            .codecs(c -> c.defaultCodecs().maxInMemorySize(4 * 1024 * 1024))
            .build();
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> schema = objectMapper.readValue(GRADING_SCHEMA_JSON, Map.class);
            this.gradingSchema = schema;
        } catch (IOException e) {
            throw new IllegalStateException("grading: failed to parse embedded JSON schema", e);
        }
    }

    public void loadLessonPrompts(String dir) {
        File directory = new File(dir);
        if (!directory.exists() || !directory.isDirectory()) {
            log.warn("grading: lesson prompts dir not found: {}", dir);
            return;
        }
        int loaded = 0;
        for (File f : directory.listFiles()) {
            if (f.isDirectory()) continue;
            String name = f.getName();
            if (!name.startsWith("lesson_") || !name.endsWith(".txt")) continue;
            try {
                int lessonId = Integer.parseInt(name.substring(7, name.length() - 4));
                String content = Files.readString(f.toPath());
                lessonPrompts.put(lessonId, content);
                loaded++;
            } catch (NumberFormatException | IOException e) {
                log.warn("grading: cannot read {}: {}", name, e.getMessage());
            }
        }
        log.info("grading: loaded {} lesson-specific prompts from {}", loaded, dir);
    }

    private String buildSystemPrompt(int lessonId) {
        String extra = lessonPrompts.get(lessonId);
        if (extra != null && !extra.isEmpty()) {
            return systemPrompt + "\n\n[DARS MATERIALLARI]\n" + extra;
        }
        return systemPrompt;
    }

    public GradingResult gradeForLesson(int lessonId, String taskText, String answerText) {
        return gradeWithPrompt(buildSystemPrompt(lessonId), taskText, answerText);
    }

    public GradingResult grade(String taskText, String answerText) {
        return gradeWithPrompt(systemPrompt, taskText, answerText);
    }

    private GradingResult gradeWithPrompt(String systemPromptText, String taskText, String answerText) {
        String userMsg = String.format(
            "[VAZIYATLI TOPSHIRIQ SHARTI]\n%s\n\n[TALABANING JAVOBI]\n%s\n",
            taskText, answerText);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("input", List.of(
            Map.of("role", "developer", "content", systemPromptText),
            Map.of("role", "user", "content", userMsg)
        ));
        requestBody.put("tools", List.of(
            Map.of("type", "file_search", "vector_store_ids", List.of(vectorStoreForeign, vectorStoreLocal))
        ));
        requestBody.put("text", Map.of(
            "format", Map.of(
                "type", "json_schema",
                "name", "grading_result",
                "schema", gradingSchema,
                "strict", true
            )
        ));

        try {
            String response = webClient.post()
                .uri("/responses")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(90))
                .block();
            return parseResponsesApiResult(response);
        } catch (Exception e) {
            log.error("grading: request failed: {}", e.getMessage());
            throw new RuntimeException("grading: " + e.getMessage(), e);
        }
    }

    private GradingResult parseResponsesApiResult(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);

            JsonNode error = root.get("error");
            if (error != null && !error.isNull()) {
                throw new RuntimeException("grading: API error: " + error.get("message").asText());
            }

            JsonNode output = root.get("output");
            if (output == null || !output.isArray()) {
                throw new RuntimeException("grading: no output array in response");
            }

            JsonNode messageItem = null;
            for (JsonNode item : output) {
                if ("message".equals(item.path("type").asText())) {
                    messageItem = item;
                    break;
                }
            }
            if (messageItem == null) {
                throw new RuntimeException("grading: no message item in output: " + response);
            }

            JsonNode content0 = messageItem.get("content").get(0);
            String jsonText = content0.get("text").asText();
            GradingJson parsed = objectMapper.readValue(jsonText, GradingJson.class);

            // Настоящие имена файлов берём из file_citation annotations самого
            // API, а не из того, что модель сама вписала в JSON — самоотчёт
            // модели ненадёжен (проверено эмпирически, см. КИТОБЛАР/test_grading.py).
            List<String> realCitations = new ArrayList<>();
            JsonNode annotations = content0.get("annotations");
            if (annotations != null && annotations.isArray()) {
                for (JsonNode ann : annotations) {
                    if ("file_citation".equals(ann.path("type").asText())) {
                        String filename = ann.path("filename").asText();
                        if (!filename.isBlank() && !realCitations.contains(filename)) {
                            realCitations.add(filename);
                        }
                    }
                }
            }

            int grade = Math.max(0, Math.min(100, parsed.grade));
            List<Criterion> criteria = parsed.criteria == null ? List.of() : parsed.criteria;

            return new GradingResult(grade, parsed.feedback, parsed.passed, criteria,
                realCitations, parsed.confidence, parsed.sourceGap);

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("grading: failed to parse response: " + e.getMessage(), e);
        }
    }

    private static class GradingJson {
        @JsonProperty("grade") int grade;
        @JsonProperty("criteria") List<Criterion> criteria;
        @JsonProperty("citations") List<String> citations;
        @JsonProperty("confidence") String confidence;
        @JsonProperty("feedback") String feedback;
        @JsonProperty("passed") boolean passed;
        @JsonProperty("source_gap") boolean sourceGap;
    }

    public static class Criterion {
        @JsonProperty("name") private String name;
        @JsonProperty("points") private int points;
        @JsonProperty("comment") private String comment;

        public Criterion() {}

        public Criterion(String name, int points, String comment) {
            this.name = name;
            this.points = points;
            this.comment = comment;
        }

        public String getName() { return name; }
        public int getPoints() { return points; }
        public String getComment() { return comment; }
    }

    public static class GradingResult {
        private final int grade;
        private final String feedback;
        private final boolean passed;
        private final List<Criterion> criteria;
        private final List<String> citations;
        private final String confidence;
        private final boolean sourceGap;

        public GradingResult(int grade, String feedback, boolean passed, List<Criterion> criteria,
                              List<String> citations, String confidence, boolean sourceGap) {
            this.grade = grade;
            this.feedback = feedback;
            this.passed = passed;
            this.criteria = criteria;
            this.citations = citations;
            this.confidence = confidence;
            this.sourceGap = sourceGap;
        }

        public int getGrade() { return grade; }
        public String getFeedback() { return feedback; }
        public boolean isPassed() { return passed; }
        public List<Criterion> getCriteria() { return criteria; }
        public List<String> getCitations() { return citations; }
        public String getConfidence() { return confidence; }
        public boolean isSourceGap() { return sourceGap; }
    }
}
