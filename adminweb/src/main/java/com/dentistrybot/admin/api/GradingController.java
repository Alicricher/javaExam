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
        int grade;
        String feedback;

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
                grade = result.getGrade();
                feedback = result.getFeedback();
            } catch (Exception e) {
                return ResponseEntity.internalServerError().body(Map.of("error", "AI grading failed: " + e.getMessage()));
            }
        } else if ("manual".equals(mode)) {
            Object gradeObj = body.get("grade");
            if (gradeObj == null) return ResponseEntity.badRequest().body(Map.of("error", "grade required for manual mode"));
            grade = Integer.parseInt(gradeObj.toString());
            if (grade < 0 || grade > 100) return ResponseEntity.badRequest().body(Map.of("error", "grade must be 0-100"));
            feedback = body.containsKey("feedback") ? (String) body.get("feedback") : "";
        } else {
            return ResponseEntity.badRequest().body(Map.of("error", "mode must be 'ai' or 'manual'"));
        }

        resultRepo.gradeSituationalAnswer(id, grade, feedback != null ? feedback : "", null);
        notificationService.notifySituationalGraded(id);
        return ResponseEntity.ok(Map.of("grade", grade, "feedback", feedback, "passed", grade >= 60));
    }
}
