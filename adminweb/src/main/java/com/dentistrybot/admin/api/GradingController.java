package com.dentistrybot.admin.api;

import com.dentistrybot.shared.model.SituationalAnswer;
import com.dentistrybot.shared.repository.LessonRepository;
import com.dentistrybot.shared.repository.ResultRepository;
import com.dentistrybot.shared.service.GradingService;
import com.dentistrybot.shared.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/results/situational")
public class GradingController {

    private final ResultRepository resultRepo;
    private final LessonRepository lessonRepo;
    private final NotificationService notificationService;
    @Nullable
    private final GradingService gradingService;

    public GradingController(ResultRepository resultRepo, LessonRepository lessonRepo,
                             NotificationService notificationService,
                             @Nullable GradingService gradingService) {
        this.resultRepo = resultRepo;
        this.lessonRepo = lessonRepo;
        this.notificationService = notificationService;
        this.gradingService = gradingService;
    }

    @PostMapping("/{id}/grade")
    public ResponseEntity<?> grade(@PathVariable int id, @RequestBody Map<String, Object> body) {
        SituationalAnswer answer = resultRepo.getSituationalAnswerById(id);
        if (answer == null) return ResponseEntity.notFound().build();

        String mode = (String) body.get("mode");

        if ("ai".equals(mode)) {
            if (gradingService == null)
                return ResponseEntity.badRequest().body(Map.of("error", "AI grading not configured"));
            try {
                var task = lessonRepo.getSituationalTaskById(answer.getTaskId());
                String taskText = task != null ? task.getTaskText() : "";
                var lesson = task != null ? lessonRepo.getLessonById(task.getLessonId()) : null;
                int lessonId = lesson != null ? lesson.getId() : 0;
                GradingService.GradingResult result = lessonId > 0
                    ? gradingService.gradeForLesson(lessonId, taskText, answer.getAnswerText())
                    : gradingService.grade(taskText, answer.getAnswerText());
                // AI-режим только возвращает предложенную оценку — в БД
                // ничего не пишем, пока админ не подтвердит через mode=manual.
                return ResponseEntity.ok(toMap(result));
            } catch (Exception e) {
                return ResponseEntity.internalServerError().body(Map.of("error", "AI grading failed: " + e.getMessage()));
            }
        } else if ("manual".equals(mode)) {
            Object gradeObj = body.get("grade");
            if (gradeObj == null) return ResponseEntity.badRequest().body(Map.of("error", "grade required for manual mode"));
            int grade = Integer.parseInt(gradeObj.toString());
            if (grade < 0 || grade > 100) return ResponseEntity.badRequest().body(Map.of("error", "grade must be 0-100"));
            String feedback = body.containsKey("feedback") ? (String) body.get("feedback") : "";
            String citations = buildCitationsString(body.get("citations"));

            resultRepo.gradeSituationalAnswer(id, grade, feedback != null ? feedback : "", null, citations);
            notificationService.notifySituationalGraded(id);
            return ResponseEntity.ok(Map.of("grade", grade, "feedback", feedback, "passed", grade >= 60));
        } else {
            return ResponseEntity.badRequest().body(Map.of("error", "mode must be 'ai' or 'manual'"));
        }
    }

    @SuppressWarnings("unchecked")
    private String buildCitationsString(Object raw) {
        if (raw == null) return "";
        if (raw instanceof java.util.List<?> list) {
            return String.join(", ", list.stream().map(Object::toString).toList());
        }
        return raw.toString();
    }

    private Map<String, Object> toMap(GradingService.GradingResult r) {
        Map<String, Object> m = new java.util.HashMap<>();
        m.put("grade", r.getGrade());
        m.put("feedback", r.getFeedback());
        m.put("passed", r.isPassed());
        m.put("criteria", r.getCriteria());
        m.put("citations", r.getCitations());
        m.put("confidence", r.getConfidence());
        m.put("sourceGap", r.isSourceGap());
        return m;
    }
}
