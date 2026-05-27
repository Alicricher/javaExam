package com.dentistrybot.shared.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GradingService {

    private static final Logger log = LoggerFactory.getLogger(GradingService.class);
    private static final Pattern JSON_BLOCK = Pattern.compile("\\{[\\s\\S]*\\}");

    private final String apiKey;
    private final String model;
    private final String systemPrompt;
    private final Map<Integer, String> lessonPrompts = new HashMap<>();
    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public GradingService(String baseUrl, String apiKey, String model, String systemPrompt, ObjectMapper objectMapper) {
        this.apiKey = apiKey;
        this.model = model;
        this.systemPrompt = systemPrompt;
        this.objectMapper = objectMapper;
        this.webClient = WebClient.builder()
            .baseUrl(baseUrl)
            .defaultHeader("Authorization", "Bearer " + apiKey)
            .defaultHeader("Content-Type", "application/json")
            .codecs(c -> c.defaultCodecs().maxInMemorySize(1024 * 1024))
            .build();
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
            "Vazifa:\n%s\n\nTalabaning javobi:\n%s\n\nFaqat JSON formatida javob ber:\n{\"grade\": <0-100>, \"feedback\": \"<o'zbek tilida 2-3 jumla>\", \"passed\": <true|false>}",
            taskText, answerText);

        Map<String, Object> requestBody = Map.of(
            "model", model,
            "messages", List.of(
                Map.of("role", "system", "content", systemPromptText),
                Map.of("role", "user", "content", userMsg)
            )
        );

        try {
            String response = webClient.post()
                .uri("/chat/completions")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(60))
                .block();

            if (response == null) throw new RuntimeException("grading: null response from API");

            var chatResp = objectMapper.readTree(response);

            var error = chatResp.get("error");
            if (error != null && !error.isNull()) {
                throw new RuntimeException("grading: API error: " + error.get("message").asText());
            }

            var choices = chatResp.get("choices");
            if (choices == null || choices.isEmpty()) {
                throw new RuntimeException("grading: empty choices in response");
            }

            String content = choices.get(0).get("message").get("content").asText();

            GradingJson result;
            try {
                result = objectMapper.readValue(content, GradingJson.class);
            } catch (Exception e) {
                // Fallback: extract JSON block if model added surrounding text
                Matcher matcher = JSON_BLOCK.matcher(content);
                if (matcher.find()) {
                    result = objectMapper.readValue(matcher.group(), GradingJson.class);
                } else {
                    throw new RuntimeException("grading: no JSON found in response: " + content);
                }
            }

            int grade = Math.max(0, Math.min(100, result.grade));
            return new GradingResult(grade, result.feedback, result.passed);

        } catch (Exception e) {
            log.error("grading: failed to grade answer: {}", e.getMessage());
            throw new RuntimeException("grading: " + e.getMessage(), e);
        }
    }

    private static class GradingJson {
        @JsonProperty("grade") int grade;
        @JsonProperty("feedback") String feedback;
        @JsonProperty("passed") boolean passed;
    }

    public static class GradingResult {
        private final int grade;
        private final String feedback;
        private final boolean passed;

        public GradingResult(int grade, String feedback, boolean passed) {
            this.grade = grade;
            this.feedback = feedback;
            this.passed = passed;
        }

        public int getGrade() { return grade; }
        public String getFeedback() { return feedback; }
        public boolean isPassed() { return passed; }
    }
}
