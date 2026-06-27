package com.dentistrybot.admin.api;

import com.dentistrybot.shared.model.*;
import com.dentistrybot.shared.repository.ResultRepository;
import com.dentistrybot.shared.repository.StudentRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/students")
public class StudentController {

    private final StudentRepository studentRepo;
    private final ResultRepository resultRepo;

    public StudentController(StudentRepository studentRepo, ResultRepository resultRepo) {
        this.studentRepo = studentRepo;
        this.resultRepo = resultRepo;
    }

    @GetMapping
    public ResponseEntity<?> list(
            @RequestParam(defaultValue = "") String name,
            @RequestParam(defaultValue = "0") int course,
            @RequestParam(defaultValue = "") String group,
            @RequestParam(defaultValue = "") String subgroup,
            @RequestParam(defaultValue = "") String faculty,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {

        var filter = new StudentFilter();
        filter.setFullName(name);
        filter.setCourse(course);
        filter.setGroupName(group);
        filter.setSubgroup(subgroup);
        filter.setFaculty(faculty);
        filter.setLimit(size);
        filter.setOffset((page - 1) * size);

        List<Student> students = studentRepo.getStudents(filter);
        int total = studentRepo.countStudents(filter);
        return ResponseEntity.ok(Map.of("students", students, "total", total, "page", page, "size", size));
    }

    @GetMapping("/filter-options")
    public ResponseEntity<?> filterOptions() {
        return ResponseEntity.ok(Map.of(
            "courses", studentRepo.getDistinctCourses(),
            "groups", studentRepo.getDistinctGroups(),
            "subgroups", studentRepo.getDistinctSubgroups(),
            "faculties", studentRepo.getDistinctFaculties()
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable int id) {
        Student s = studentRepo.getStudentById(id);
        if (s == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(s);
    }

    @PutMapping("/{id}/name")
    public ResponseEntity<?> updateName(@PathVariable int id, @RequestBody Map<String, String> body) {
        String name = body.get("name");
        if (name == null || name.isBlank()) return ResponseEntity.badRequest().body(Map.of("error", "name required"));
        Student s = studentRepo.getStudentById(id);
        if (s == null) return ResponseEntity.notFound().build();
        studentRepo.updateStudentName(id, name.trim());
        return ResponseEntity.ok(Map.of("ok", true));
    }

    @GetMapping("/{id}/results")
    public ResponseEntity<?> results(@PathVariable int id,
                                     @RequestParam(defaultValue = "1") int page,
                                     @RequestParam(defaultValue = "20") int size) {
        List<TestResultWithStudent> results = resultRepo.getTestResultsByStudentId(id, size, (page - 1) * size);
        int total = resultRepo.countTestResultsByStudentId(id);
        return ResponseEntity.ok(Map.of("results", results, "total", total, "page", page, "size", size));
    }

    @PostMapping("/{id}/retakes/test")
    public ResponseEntity<?> grantTestRetake(@PathVariable int id, @RequestBody Map<String, Object> body) {
        Object testIdObj = body.get("testId");
        if (testIdObj == null) return ResponseEntity.badRequest().body(Map.of("error", "testId required"));
        int testId = Integer.parseInt(testIdObj.toString());
        Student s = studentRepo.getStudentById(id);
        if (s == null) return ResponseEntity.notFound().build();
        var retake = new TestRetake();
        retake.setStudentId(id);
        retake.setTestId(testId);
        resultRepo.createTestRetake(retake);
        return ResponseEntity.ok(Map.of("ok", true));
    }

    @PostMapping("/{id}/retakes/situational")
    public ResponseEntity<?> grantSituationalRetake(@PathVariable int id, @RequestBody Map<String, Object> body) {
        Object taskIdObj = body.get("taskId");
        if (taskIdObj == null) return ResponseEntity.badRequest().body(Map.of("error", "taskId required"));
        int taskId = Integer.parseInt(taskIdObj.toString());
        Student s = studentRepo.getStudentById(id);
        if (s == null) return ResponseEntity.notFound().build();
        var retake = new SituationalRetake();
        retake.setStudentId(id);
        retake.setTaskId(taskId);
        resultRepo.createSituationalRetake(retake);
        return ResponseEntity.ok(Map.of("ok", true));
    }
}
