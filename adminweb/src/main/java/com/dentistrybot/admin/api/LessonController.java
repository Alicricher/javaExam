package com.dentistrybot.admin.api;

import com.dentistrybot.admin.security.AccessControlService;
import com.dentistrybot.shared.model.Lesson;
import com.dentistrybot.shared.repository.LessonRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/lessons")
public class LessonController {

    private final LessonRepository lessonRepo;
    private final AccessControlService accessControl;

    public LessonController(LessonRepository lessonRepo, AccessControlService accessControl) {
        this.lessonRepo = lessonRepo;
        this.accessControl = accessControl;
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable int id) {
        Lesson l = lessonRepo.getLessonById(id);
        if (l == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(l);
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Map<String, Object> body, Authentication auth) {
        Object unitIdObj = body.get("unitId");
        String titleUz = (String) body.get("titleUz");
        if (unitIdObj == null || titleUz == null || titleUz.isBlank())
            return ResponseEntity.badRequest().body(Map.of("error", "unitId and titleUz required"));
        int unitId = Integer.parseInt(unitIdObj.toString());
        if (!accessControl.canManageUnit(auth, unitId)) return ResponseEntity.status(403).body(Map.of("error", "Forbidden"));

        List<Lesson> existing = lessonRepo.getLessonsByUnitId(unitId);
        int nextNum = existing.stream().mapToInt(Lesson::getLessonNumber).max().orElse(0) + 1;

        var lesson = new Lesson();
        lesson.setUnitId(unitId);
        lesson.setLessonNumber(nextNum);
        lesson.setTitleUz(titleUz.trim());
        return ResponseEntity.ok(lessonRepo.createLesson(lesson));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable int id, @RequestBody Map<String, String> body, Authentication auth) {
        Lesson l = lessonRepo.getLessonById(id);
        if (l == null) return ResponseEntity.notFound().build();
        if (!accessControl.canManageUnit(auth, l.getUnitId())) return ResponseEntity.status(403).body(Map.of("error", "Forbidden"));
        String titleUz = body.get("titleUz");
        if (titleUz == null || titleUz.isBlank())
            return ResponseEntity.badRequest().body(Map.of("error", "titleUz required"));
        l.setTitleUz(titleUz.trim());
        lessonRepo.updateLesson(l);
        return ResponseEntity.ok(l);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable int id, Authentication auth) {
        Lesson l = lessonRepo.getLessonById(id);
        if (l == null) return ResponseEntity.notFound().build();
        if (!accessControl.canManageUnit(auth, l.getUnitId())) return ResponseEntity.status(403).body(Map.of("error", "Forbidden"));
        int unitId = l.getUnitId();
        lessonRepo.deleteLesson(id);
        lessonRepo.renumberLessons(unitId);
        return ResponseEntity.ok(Map.of("ok", true));
    }

    @GetMapping("/{id}/theory")
    public ResponseEntity<?> theory(@PathVariable int id) {
        return ResponseEntity.ok(lessonRepo.getTheoryMaterialsByLessonId(id));
    }

    @GetMapping("/{id}/tasks")
    public ResponseEntity<?> tasks(@PathVariable int id) {
        return ResponseEntity.ok(lessonRepo.getSituationalTasksByLessonId(id));
    }
}
