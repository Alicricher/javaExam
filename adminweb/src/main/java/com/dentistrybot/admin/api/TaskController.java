package com.dentistrybot.admin.api;

import com.dentistrybot.shared.model.SituationalTask;
import com.dentistrybot.shared.repository.LessonRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final LessonRepository lessonRepo;

    public TaskController(LessonRepository lessonRepo) {
        this.lessonRepo = lessonRepo;
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Map<String, Object> body) {
        Object lessonIdObj = body.get("lessonId");
        String taskText = (String) body.get("taskText");
        Object timeObj = body.get("timeLimitMinutes");
        if (lessonIdObj == null || taskText == null || taskText.isBlank())
            return ResponseEntity.badRequest().body(Map.of("error", "lessonId and taskText required"));
        int lessonId = Integer.parseInt(lessonIdObj.toString());
        int timeLimit = timeObj != null ? Integer.parseInt(timeObj.toString()) : 30;
        int orderNum = lessonRepo.getNextTaskOrderNum(lessonId);
        var task = new SituationalTask();
        task.setLessonId(lessonId);
        task.setTaskText(taskText.trim());
        task.setTimeLimitMinutes(timeLimit);
        task.setOrderNum(orderNum);
        return ResponseEntity.ok(lessonRepo.createSituationalTask(task));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable int id, @RequestBody Map<String, Object> body) {
        SituationalTask t = lessonRepo.getSituationalTaskById(id);
        if (t == null) return ResponseEntity.notFound().build();
        if (body.containsKey("taskText"))
            lessonRepo.updateSituationalTaskText(id, ((String) body.get("taskText")).trim());
        if (body.containsKey("timeLimitMinutes"))
            lessonRepo.updateSituationalTaskTime(id, Integer.parseInt(body.get("timeLimitMinutes").toString()));
        return ResponseEntity.ok(lessonRepo.getSituationalTaskById(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable int id) {
        SituationalTask t = lessonRepo.getSituationalTaskById(id);
        if (t == null) return ResponseEntity.notFound().build();
        lessonRepo.deleteSituationalTask(id);
        lessonRepo.renumberSituationalTasks(t.getLessonId());
        return ResponseEntity.ok(Map.of("ok", true));
    }
}
