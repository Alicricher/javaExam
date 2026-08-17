package com.dentistrybot.admin.api;

import com.dentistrybot.shared.model.*;
import com.dentistrybot.shared.repository.LessonRepository;
import com.dentistrybot.shared.repository.TestRepository;
import com.dentistrybot.shared.service.FileService;
import com.dentistrybot.shared.service.ImportService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.List;
import java.util.Map;

@RestController
public class TestController {

    private final TestRepository testRepo;
    private final LessonRepository lessonRepo;
    private final ImportService importService;
    private final FileService fileService;

    public TestController(TestRepository testRepo, LessonRepository lessonRepo,
                          ImportService importService, FileService fileService) {
        this.testRepo = testRepo;
        this.lessonRepo = lessonRepo;
        this.importService = importService;
        this.fileService = fileService;
    }

    // ===== TESTS =====

    @GetMapping("/api/lessons/{lessonId}/test")
    public ResponseEntity<?> getTest(@PathVariable int lessonId) {
        Test t = testRepo.getTestByLessonId(lessonId);
        if (t == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(t);
    }

    @PostMapping("/api/tests")
    public ResponseEntity<?> createTest(@RequestBody Map<String, Object> body) {
        Object lessonIdObj = body.get("lessonId");
        Object timeObj = body.get("timeLimitMinutes");
        if (lessonIdObj == null) return ResponseEntity.badRequest().body(Map.of("error", "lessonId required"));
        int lessonId = Integer.parseInt(lessonIdObj.toString());
        int timeLimit = timeObj != null ? Integer.parseInt(timeObj.toString()) : 30;
        var test = new Test();
        test.setLessonId(lessonId);
        test.setTitleUz("Test");
        test.setTimeLimitMinutes(timeLimit);
        test.setTotalPoints(0);
        return ResponseEntity.ok(testRepo.createTest(test));
    }

    @PutMapping("/api/tests/{id}")
    public ResponseEntity<?> updateTest(@PathVariable int id, @RequestBody Map<String, Object> body) {
        Test t = testRepo.getTestById(id);
        if (t == null) return ResponseEntity.notFound().build();
        if (body.containsKey("timeLimitMinutes"))
            t.setTimeLimitMinutes(Integer.parseInt(body.get("timeLimitMinutes").toString()));
        if (body.containsKey("titleUz"))
            t.setTitleUz((String) body.get("titleUz"));
        testRepo.updateTest(t);
        return ResponseEntity.ok(t);
    }

    @DeleteMapping("/api/tests/{id}")
    public ResponseEntity<?> deleteTest(@PathVariable int id) {
        Test t = testRepo.getTestById(id);
        if (t == null) return ResponseEntity.notFound().build();
        testRepo.deleteTest(id);
        return ResponseEntity.ok(Map.of("ok", true));
    }

    // ===== QUESTIONS =====

    @GetMapping("/api/tests/{testId}/questions")
    public ResponseEntity<?> getQuestions(@PathVariable int testId,
                                          @RequestParam(defaultValue = "1") int page,
                                          @RequestParam(defaultValue = "20") int size) {
        List<Question> questions = testRepo.getQuestionsByTestIdPaginated(testId, size, (page - 1) * size);
        int total = testRepo.countQuestions(testId);
        return ResponseEntity.ok(Map.of("questions", questions, "total", total, "page", page, "size", size));
    }

    @GetMapping("/api/questions/{id}")
    public ResponseEntity<?> getQuestion(@PathVariable int id) {
        QuestionWithOptions q = testRepo.getQuestionWithOptions(id);
        if (q == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(q);
    }

    @PostMapping("/api/tests/{testId}/questions")
    public ResponseEntity<?> createQuestion(@PathVariable int testId, @RequestBody Map<String, Object> body) {
        Test test = testRepo.getTestById(testId);
        if (test == null || !test.isActive()) return ResponseEntity.notFound().build();

        String questionText = (String) body.get("questionText");
        if (questionText == null || questionText.isBlank())
            return ResponseEntity.badRequest().body(Map.of("error", "questionText required"));

        int points = body.containsKey("points") ? Integer.parseInt(body.get("points").toString()) : 1;
        if (points <= 0) return ResponseEntity.badRequest().body(Map.of("error", "points must be positive"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> options = (List<Map<String, Object>>) body.get("options");
        if (options == null || options.size() < 2)
            return ResponseEntity.badRequest().body(Map.of("error", "at least 2 options required"));

        long correctCount = options.stream().filter(o -> Boolean.parseBoolean(String.valueOf(o.get("isCorrect")))).count();
        if (correctCount != 1)
            return ResponseEntity.badRequest().body(Map.of("error", "exactly one correct option required"));

        Question q = new Question();
        q.setTestId(testId);
        q.setQuestionText(questionText.trim());
        q.setPoints(points);
        q.setOrderNum(testRepo.getNextQuestionOrderNum(testId));
        testRepo.createQuestion(q);

        int order = 1;
        for (Map<String, Object> optBody : options) {
            String text = String.valueOf(optBody.getOrDefault("optionText", "")).trim();
            if (text.isEmpty()) continue;
            AnswerOption option = new AnswerOption();
            option.setQuestionId(q.getId());
            option.setOptionText(text);
            option.setCorrect(Boolean.parseBoolean(String.valueOf(optBody.get("isCorrect"))));
            option.setOrderNum(order++);
            testRepo.createAnswerOption(option);
        }

        return ResponseEntity.ok(testRepo.getQuestionWithOptions(q.getId()));
    }

    @PutMapping("/api/questions/{id}")
    public ResponseEntity<?> updateQuestion(@PathVariable int id, @RequestBody Map<String, Object> body) {
        Question q = testRepo.getQuestionById(id);
        if (q == null) return ResponseEntity.notFound().build();
        if (body.containsKey("questionText")) q.setQuestionText((String) body.get("questionText"));
        if (body.containsKey("points")) q.setPoints(Integer.parseInt(body.get("points").toString()));
        testRepo.updateQuestion(q);
        return ResponseEntity.ok(testRepo.getQuestionWithOptions(id));
    }

    @DeleteMapping("/api/questions/{id}")
    public ResponseEntity<?> deleteQuestion(@PathVariable int id) {
        Question q = testRepo.getQuestionById(id);
        if (q == null) return ResponseEntity.notFound().build();
        testRepo.deleteQuestion(id, q.getTestId());
        return ResponseEntity.ok(Map.of("ok", true));
    }

    @PostMapping("/api/questions/{id}/photo")
    public ResponseEntity<?> uploadQuestionPhoto(@PathVariable int id,
                                                  @RequestParam("file") MultipartFile file) {
        Question q = testRepo.getQuestionById(id);
        if (q == null) return ResponseEntity.notFound().build();
        if (file.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error", "file required"));
        try {
            String original = file.getOriginalFilename() != null ? file.getOriginalFilename() : "photo.jpg";
            String relativePath = fileService.savePhoto("photos/questions", "q_" + id + "_" + original, file.getBytes());
            if (q.getPhotoFilePath() != null) {
                try { fileService.deleteFile(q.getPhotoFilePath()); } catch (Exception ignored) {}
            }
            testRepo.updateQuestionPhotoFilePath(id, relativePath);
            return ResponseEntity.ok(Map.of("photoFilePath", relativePath));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/api/questions/{id}/photo")
    public ResponseEntity<?> deleteQuestionPhoto(@PathVariable int id) {
        Question q = testRepo.getQuestionById(id);
        if (q == null) return ResponseEntity.notFound().build();
        try {
            if (q.getPhotoFilePath() != null) {
                try { fileService.deleteFile(q.getPhotoFilePath()); } catch (Exception ignored) {}
            }
            testRepo.updateQuestionPhotoFilePath(id, null);
            return ResponseEntity.ok(Map.of("ok", true));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/api/questions/{id}/move")
    public ResponseEntity<?> moveQuestion(@PathVariable int id, @RequestBody Map<String, String> body) {
        Question q = testRepo.getQuestionById(id);
        if (q == null) return ResponseEntity.notFound().build();
        String direction = body.get("direction");
        List<Question> all = testRepo.getQuestionsByTestId(q.getTestId());
        int idx = -1;
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).getId() == id) { idx = i; break; }
        }
        if ("up".equals(direction) && idx > 0) {
            testRepo.swapQuestionOrders(id, all.get(idx - 1).getId());
        } else if ("down".equals(direction) && idx >= 0 && idx < all.size() - 1) {
            testRepo.swapQuestionOrders(id, all.get(idx + 1).getId());
        }
        return ResponseEntity.ok(Map.of("ok", true));
    }

    @DeleteMapping("/api/tests/{testId}/questions")
    public ResponseEntity<?> clearQuestions(@PathVariable int testId) {
        testRepo.deleteAllQuestions(testId);
        return ResponseEntity.ok(Map.of("ok", true));
    }

    // ===== ANSWER OPTIONS =====

    @PutMapping("/api/questions/{questionId}/options/{optId}")
    public ResponseEntity<?> updateOption(@PathVariable int questionId,
                                          @PathVariable int optId,
                                          @RequestBody Map<String, Object> body) {
        AnswerOption opt = testRepo.getAnswerOptionById(optId);
        if (opt == null) return ResponseEntity.notFound().build();
        if (body.containsKey("optionText")) opt.setOptionText((String) body.get("optionText"));
        if (body.containsKey("isCorrect")) opt.setCorrect(Boolean.parseBoolean(body.get("isCorrect").toString()));
        testRepo.updateAnswerOption(opt);
        return ResponseEntity.ok(opt);
    }

    @PutMapping("/api/questions/{questionId}/correct")
    public ResponseEntity<?> setCorrect(@PathVariable int questionId, @RequestBody Map<String, Object> body) {
        Object optIdObj = body.get("optionId");
        if (optIdObj == null) return ResponseEntity.badRequest().body(Map.of("error", "optionId required"));
        testRepo.setCorrectAnswer(questionId, Integer.parseInt(optIdObj.toString()));
        return ResponseEntity.ok(Map.of("ok", true));
    }

    // ===== IMPORT / TEMPLATE =====

    @PostMapping("/api/tests/{testId}/questions/import")
    public ResponseEntity<?> importQuestions(@PathVariable int testId,
                                             @RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error", "file required"));
        String originalName = file.getOriginalFilename();
        try {
            File tmp = File.createTempFile("import_", originalName != null ? originalName : ".xlsx");
            file.transferTo(tmp);
            int count;
            if (originalName != null && originalName.toLowerCase().endsWith(".csv")) {
                count = importService.importQuestionsFromCsv(tmp.getAbsolutePath(), testId);
            } else {
                count = importService.importQuestionsFromExcel(tmp.getAbsolutePath(), testId);
            }
            tmp.delete();
            return ResponseEntity.ok(Map.of("imported", count));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/api/tests/{testId}/questions/template")
    public void downloadTemplate(@PathVariable int testId, HttpServletResponse response) throws Exception {
        File tmp = File.createTempFile("template_", ".xlsx");
        importService.generateQuestionTemplate(tmp.getAbsolutePath());
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=questions_template.xlsx");
        java.nio.file.Files.copy(tmp.toPath(), response.getOutputStream());
        response.getOutputStream().flush();
        tmp.delete();
    }
}
