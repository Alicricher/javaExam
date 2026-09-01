package com.dentistrybot.admin.api;

import com.dentistrybot.admin.security.AccessControlService;
import com.dentistrybot.shared.model.Lesson;
import com.dentistrybot.shared.model.TheoryMaterial;
import com.dentistrybot.shared.repository.LessonRepository;
import com.dentistrybot.shared.service.FileService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

@RestController
@RequestMapping("/api/theory")
public class TheoryController {

    private final LessonRepository lessonRepo;
    private final FileService fileService;
    private final AccessControlService accessControl;

    public TheoryController(LessonRepository lessonRepo, FileService fileService, AccessControlService accessControl) {
        this.lessonRepo = lessonRepo;
        this.fileService = fileService;
        this.accessControl = accessControl;
    }

    private boolean forbidden(Authentication auth, int lessonId) {
        Integer unitId = lessonRepo.getUnitIdForLesson(lessonId);
        return unitId == null || !accessControl.canManageUnit(auth, unitId);
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestParam int lessonId,
                                    @RequestParam String titleUz,
                                    @RequestParam(defaultValue = "material") String materialType,
                                    @RequestParam(required = false) String description,
                                    @RequestParam(required = false) MultipartFile file,
                                    Authentication auth) {
        Lesson lesson = lessonRepo.getLessonById(lessonId);
        if (lesson == null) return ResponseEntity.badRequest().body(Map.of("error", "lesson not found"));
        if (!accessControl.canManageUnit(auth, lesson.getUnitId())) return ResponseEntity.status(403).body(Map.of("error", "Forbidden"));

        String filePath = null;
        if (file != null && !file.isEmpty()) {
            try {
                Lesson l = lessonRepo.getLessonById(lessonId);
                // Need unit name for the directory
                var unit = lessonRepo.getUnitById(l.getUnitId());
                String unitName = unit != null ? unit.getName() : "unit";
                fileService.ensureMaterialsDir(unitName, l.getLessonNumber());
                String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "file";
                filePath = fileService.saveFile(unitName, l.getLessonNumber(), originalName, file.getBytes());
            } catch (Exception e) {
                return ResponseEntity.internalServerError().body(Map.of("error", "file save failed: " + e.getMessage()));
            }
        }

        if (titleUz == null || titleUz.isBlank())
            return ResponseEntity.badRequest().body(Map.of("error", "titleUz required"));

        var material = new TheoryMaterial();
        material.setLessonId(lessonId);
        material.setTitleUz(titleUz.trim());
        material.setMaterialType(materialType);
        material.setDescription(description != null ? description.trim() : "");
        material.setFilePath(filePath != null ? filePath : "");
        return ResponseEntity.ok(lessonRepo.createTheoryMaterial(material));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable int id,
                                    @RequestParam(required = false) String titleUz,
                                    @RequestParam(required = false) String materialType,
                                    @RequestParam(required = false) String description,
                                    @RequestParam(required = false) MultipartFile file,
                                    @RequestBody(required = false) Map<String, String> body,
                                    Authentication auth) {
        TheoryMaterial m = lessonRepo.getTheoryMaterialById(id);
        if (m == null) return ResponseEntity.notFound().build();
        if (forbidden(auth, m.getLessonId())) return ResponseEntity.status(403).body(Map.of("error", "Forbidden"));

        if (body != null) {
            if (body.containsKey("titleUz")) titleUz = body.get("titleUz");
            if (body.containsKey("materialType")) materialType = body.get("materialType");
            if (body.containsKey("description")) description = body.get("description");
        }

        if (titleUz != null) {
            if (titleUz.isBlank()) return ResponseEntity.badRequest().body(Map.of("error", "titleUz required"));
            lessonRepo.updateTheoryMaterialTitle(id, titleUz.trim());
        }
        if (materialType != null) lessonRepo.updateTheoryMaterialType(id, materialType);
        if (description != null) lessonRepo.updateTheoryMaterialDescription(id, description.trim());
        if (file != null && !file.isEmpty()) {
            try {
                Lesson lesson = lessonRepo.getLessonById(m.getLessonId());
                var unit = lesson != null ? lessonRepo.getUnitById(lesson.getUnitId()) : null;
                String unitName = unit != null ? unit.getName() : "unit";
                int lessonNumber = lesson != null ? lesson.getLessonNumber() : m.getLessonId();
                String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "file";
                String newPath = fileService.saveFile(unitName, lessonNumber, originalName, file.getBytes());
                if (m.getFilePath() != null && !m.getFilePath().isEmpty()) {
                    try { fileService.deleteFile(m.getFilePath()); } catch (Exception ignored) {}
                }
                lessonRepo.updateTheoryMaterialFilePath(id, newPath);
            } catch (Exception e) {
                return ResponseEntity.internalServerError().body(Map.of("error", "file save failed: " + e.getMessage()));
            }
        }
        return ResponseEntity.ok(lessonRepo.getTheoryMaterialById(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable int id, Authentication auth) {
        TheoryMaterial m = lessonRepo.getTheoryMaterialById(id);
        if (m == null) return ResponseEntity.notFound().build();
        if (forbidden(auth, m.getLessonId())) return ResponseEntity.status(403).body(Map.of("error", "Forbidden"));
        if (m.getFilePath() != null && !m.getFilePath().isEmpty()) {
            try { fileService.deleteFile(m.getFilePath()); } catch (Exception ignored) {}
        }
        lessonRepo.deleteTheoryMaterial(id);
        return ResponseEntity.ok(Map.of("ok", true));
    }

    @GetMapping("/{id}/file")
    public void downloadFile(@PathVariable int id, HttpServletResponse response) throws Exception {
        TheoryMaterial m = lessonRepo.getTheoryMaterialById(id);
        if (m == null || m.getFilePath() == null || m.getFilePath().isEmpty()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        String fullPath = fileService.getFilePath(m.getFilePath());
        Path path = Path.of(fullPath);
        if (!Files.exists(path)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        String contentType = Files.probeContentType(path);
        if (contentType == null) contentType = "application/octet-stream";
        response.setContentType(contentType);
        response.setHeader("Content-Disposition", "attachment; filename=\"" + path.getFileName() + "\"");
        try (InputStream in = Files.newInputStream(path)) {
            in.transferTo(response.getOutputStream());
        }
    }
}
