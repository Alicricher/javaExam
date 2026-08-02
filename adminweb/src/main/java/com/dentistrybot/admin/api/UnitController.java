package com.dentistrybot.admin.api;

import com.dentistrybot.shared.model.Unit;
import com.dentistrybot.shared.repository.LessonRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/units")
public class UnitController {

    private static final int MAX_NAME_LENGTH = 10;

    private final LessonRepository lessonRepo;

    public UnitController(LessonRepository lessonRepo) {
        this.lessonRepo = lessonRepo;
    }

    @GetMapping
    public ResponseEntity<?> list() {
        return ResponseEntity.ok(lessonRepo.getAllUnits());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable int id) {
        Unit u = lessonRepo.getUnitById(id);
        if (u == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(u);
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Map<String, String> body) {
        String name = body.get("name");
        String titleUz = body.get("titleUz");
        if (name == null || titleUz == null || name.isBlank() || titleUz.isBlank())
            return ResponseEntity.badRequest().body(Map.of("error", "name and titleUz required"));
        if (name.trim().length() > MAX_NAME_LENGTH)
            return ResponseEntity.badRequest().body(Map.of("error", "Kod " + MAX_NAME_LENGTH + " ta belgidan oshmasligi kerak"));
        if (lessonRepo.checkUnitNameExists(name.trim(), -1))
            return ResponseEntity.badRequest().body(Map.of("error", "Unit name already exists"));
        var unit = new Unit();
        unit.setName(name.trim().toUpperCase());
        unit.setTitleUz(titleUz.trim());
        return ResponseEntity.ok(lessonRepo.createUnit(unit));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable int id, @RequestBody Map<String, String> body) {
        Unit u = lessonRepo.getUnitById(id);
        if (u == null) return ResponseEntity.notFound().build();
        String name = body.getOrDefault("name", u.getName());
        String titleUz = body.getOrDefault("titleUz", u.getTitleUz());
        if (name.trim().length() > MAX_NAME_LENGTH)
            return ResponseEntity.badRequest().body(Map.of("error", "Kod " + MAX_NAME_LENGTH + " ta belgidan oshmasligi kerak"));
        if (!name.equals(u.getName()) && lessonRepo.checkUnitNameExists(name.trim(), id))
            return ResponseEntity.badRequest().body(Map.of("error", "Unit name already exists"));
        u.setName(name.trim().toUpperCase());
        u.setTitleUz(titleUz.trim());
        lessonRepo.updateUnit(u);
        return ResponseEntity.ok(u);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable int id) {
        Unit u = lessonRepo.getUnitById(id);
        if (u == null) return ResponseEntity.notFound().build();
        lessonRepo.deleteUnit(id);
        return ResponseEntity.ok(Map.of("ok", true));
    }

    @GetMapping("/{id}/lessons")
    public ResponseEntity<?> lessons(@PathVariable int id) {
        return ResponseEntity.ok(lessonRepo.getLessonsByUnitId(id));
    }
}
