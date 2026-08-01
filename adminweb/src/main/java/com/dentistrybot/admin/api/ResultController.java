package com.dentistrybot.admin.api;

import com.dentistrybot.shared.model.TestResultWithStudent;
import com.dentistrybot.shared.repository.ResultRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/results")
public class ResultController {

    private final ResultRepository resultRepo;

    public ResultController(ResultRepository resultRepo) {
        this.resultRepo = resultRepo;
    }

    @GetMapping("/tests")
    public ResponseEntity<?> testResults(
            @RequestParam(defaultValue = "") String studentName,
            @RequestParam(defaultValue = "") String group,
            @RequestParam(defaultValue = "") String subgroup,
            @RequestParam(defaultValue = "") String testName,
            @RequestParam(defaultValue = "") String status,
            @RequestParam(required = false) Integer unitId,
            @RequestParam(required = false) Integer lessonId,
            @RequestParam(required = false) Integer testId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {

        int offset = (page - 1) * size;
        List<TestResultWithStudent> results = resultRepo.getTestResultsFiltered(
            group, subgroup, studentName, testName, status, unitId, lessonId, testId, size, offset);
        int total = resultRepo.countTestResultsFiltered(group, subgroup, studentName, testName, status, unitId, lessonId, testId);

        return ResponseEntity.ok(Map.of("results", results, "total", total, "page", page, "size", size));
    }

    @GetMapping("/tests/{id}/answers")
    public ResponseEntity<?> testResultAnswers(@PathVariable int id) {
        var result = resultRepo.getTestResultById(id);
        if (result == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(resultRepo.getTestAnswerDetailsByResultId(id));
    }

    @GetMapping("/situational")
    public ResponseEntity<?> situationalResults(
            @RequestParam(defaultValue = "") String studentName,
            @RequestParam(defaultValue = "") String group,
            @RequestParam(defaultValue = "") String subgroup,
            @RequestParam(defaultValue = "") String graded,
            @RequestParam(required = false) Integer unitId,
            @RequestParam(required = false) Integer lessonId,
            @RequestParam(required = false) Integer taskId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {

        int offset = (page - 1) * size;

        var results = resultRepo.getSituationalAnswersFiltered(
            group, subgroup, studentName, graded, unitId, lessonId, taskId, size, offset);
        int total = resultRepo.countSituationalAnswersFiltered(
            group, subgroup, studentName, graded, unitId, lessonId, taskId);

        return ResponseEntity.ok(Map.of("results", results, "total", total, "page", page, "size", size));
    }

    @GetMapping("/situational/{id}")
    public ResponseEntity<?> getSituationalAnswer(@PathVariable int id) {
        var answer = resultRepo.getSituationalAnswerById(id);
        if (answer == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(answer);
    }
}
