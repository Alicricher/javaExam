package com.dentistrybot.shared.repository;

import com.dentistrybot.shared.model.*;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class ResultRepository {

    private final NamedParameterJdbcTemplate jdbc;
    private final TransactionTemplate tx;

    public ResultRepository(NamedParameterJdbcTemplate jdbc, TransactionTemplate tx) {
        this.jdbc = jdbc;
        this.tx = tx;
    }

    private static final RowMapper<TestResult> RESULT_MAPPER = (rs, rowNum) -> {
        TestResult r = new TestResult();
        r.setId(rs.getInt("id"));
        r.setStudentId(rs.getInt("student_id"));
        r.setTestId(rs.getInt("test_id"));
        r.setScore(rs.getInt("score"));
        r.setMaxScore(rs.getInt("max_score"));
        r.setStartedAt(rs.getObject("started_at", LocalDateTime.class));
        r.setCompletedAt(rs.getObject("completed_at", LocalDateTime.class));
        r.setStatus(rs.getString("status"));
        return r;
    };

    private static final RowMapper<TestResultWithStudent> RESULT_WITH_STUDENT_MAPPER = (rs, rowNum) -> {
        TestResultWithStudent r = new TestResultWithStudent();
        r.setId(rs.getInt("id"));
        r.setStudentId(rs.getInt("student_id"));
        r.setTestId(rs.getInt("test_id"));
        r.setScore(rs.getInt("score"));
        r.setMaxScore(rs.getInt("max_score"));
        r.setStartedAt(rs.getObject("started_at", LocalDateTime.class));
        r.setCompletedAt(rs.getObject("completed_at", LocalDateTime.class));
        r.setStatus(rs.getString("status"));
        r.setStudentName(rs.getString("student_name"));
        r.setTestTitle(rs.getString("test_title"));
        r.setLessonTitle(rs.getString("lesson_title"));
        r.setUnitName(rs.getString("unit_name"));
        try { r.setAttemptNumber(rs.getInt("attempt_number")); } catch (Exception ignored) {}
        return r;
    };

    private static final RowMapper<SituationalAnswerWithStudent> SIT_MAPPER = (rs, rowNum) -> {
        SituationalAnswerWithStudent a = new SituationalAnswerWithStudent();
        a.setId(rs.getInt("id"));
        a.setStudentId(rs.getInt("student_id"));
        a.setTaskId(rs.getInt("task_id"));
        a.setAnswerText(rs.getString("answer_text"));
        a.setPhotoFileId(rs.getString("photo_file_id"));
        a.setSubmittedAt(rs.getObject("submitted_at", LocalDateTime.class));
        a.setGraded(rs.getBoolean("is_graded"));
        int grade = rs.getInt("grade");
        a.setGrade(rs.wasNull() ? null : grade);
        a.setFeedback(rs.getString("feedback"));
        int gradedBy = rs.getInt("graded_by");
        a.setGradedBy(rs.wasNull() ? null : gradedBy);
        a.setGradedAt(rs.getObject("graded_at", LocalDateTime.class));
        a.setStudentName(rs.getString("student_name"));
        a.setTaskText(rs.getString("task_text"));
        a.setLessonTitle(rs.getString("lesson_title"));
        a.setUnitName(rs.getString("unit_name"));
        return a;
    };

    // ==================== TEST RESULTS ====================

    public TestResult getTestResultById(int id) {
        var results = jdbc.query("""
            SELECT id, student_id, test_id, score, max_score, started_at, completed_at, status
            FROM test_results WHERE id = :id
            """, Map.of("id", id), RESULT_MAPPER);
        return results.isEmpty() ? null : results.get(0);
    }

    public TestResult getActiveTestResult(int studentId, int testId) {
        var results = jdbc.query("""
            SELECT id, student_id, test_id, score, max_score, started_at, completed_at, status
            FROM test_results
            WHERE student_id = :studentId AND test_id = :testId AND status = 'in_progress'
            ORDER BY started_at DESC LIMIT 1
            """, Map.of("studentId", studentId, "testId", testId), RESULT_MAPPER);
        return results.isEmpty() ? null : results.get(0);
    }

    public void updateTestResult(TestResult result) {
        jdbc.update("""
            UPDATE test_results SET score = :score, completed_at = :completedAt, status = :status WHERE id = :id
            """,
            Map.of("id", result.getId(), "score", result.getScore(),
                   "completedAt", result.getCompletedAt(), "status", result.getStatus()));
    }

    public boolean hasCompletedTest(int studentId, int testId) {
        Boolean exists = jdbc.queryForObject("""
            SELECT EXISTS(
                SELECT 1 FROM test_results
                WHERE student_id = :studentId AND test_id = :testId AND status IN ('completed', 'timeout')
            )
            """, Map.of("studentId", studentId, "testId", testId), Boolean.class);
        return Boolean.TRUE.equals(exists);
    }

    public boolean hasAvailableRetake(int studentId, int testId) {
        Boolean exists = jdbc.queryForObject("""
            SELECT EXISTS(SELECT 1 FROM test_retakes WHERE student_id = :studentId AND test_id = :testId AND used = FALSE)
            """, Map.of("studentId", studentId, "testId", testId), Boolean.class);
        return Boolean.TRUE.equals(exists);
    }

    public List<TestResult> getTestResultsByStudentId(int studentId) {
        return jdbc.query("""
            SELECT id, student_id, test_id, score, max_score, started_at, completed_at, status
            FROM test_results WHERE student_id = :studentId ORDER BY started_at DESC
            """, Map.of("studentId", studentId), RESULT_MAPPER);
    }

    public List<TestResultWithStudent> getTestResultsByStudentIdWithDetails(int studentId) {
        return jdbc.query("""
            SELECT tr.id, tr.student_id, tr.test_id, tr.score, tr.max_score,
                   tr.started_at, tr.completed_at, tr.status,
                   s.full_name as student_name, t.title_uz as test_title,
                   l.title_uz as lesson_title, u.name as unit_name
            FROM test_results tr
            JOIN students s ON tr.student_id = s.id
            JOIN tests t ON tr.test_id = t.id
            JOIN lessons l ON t.lesson_id = l.id
            JOIN units u ON l.unit_id = u.id
            WHERE tr.student_id = :studentId ORDER BY tr.started_at DESC
            """, Map.of("studentId", studentId), RESULT_WITH_STUDENT_MAPPER);
    }

    public List<TestResult> getAllTestAttempts(int studentId, int testId) {
        return jdbc.query("""
            SELECT id, student_id, test_id, score, max_score, started_at, completed_at, status
            FROM test_results WHERE student_id = :studentId AND test_id = :testId ORDER BY started_at ASC
            """, Map.of("studentId", studentId, "testId", testId), RESULT_MAPPER);
    }

    public List<TestResultWithStudent> getTestResultsWithStudents(int limit, int offset) {
        return jdbc.query("""
            WITH attempt_numbers AS (
                SELECT id, ROW_NUMBER() OVER (PARTITION BY student_id, test_id ORDER BY started_at ASC) as attempt_number
                FROM test_results
            )
            SELECT tr.id, tr.student_id, tr.test_id, tr.score, tr.max_score,
                   tr.started_at, tr.completed_at, tr.status,
                   s.full_name as student_name, t.title_uz as test_title,
                   l.title_uz as lesson_title, u.name as unit_name,
                   COALESCE(an.attempt_number, 1) as attempt_number
            FROM test_results tr
            JOIN students s ON tr.student_id = s.id
            JOIN tests t ON tr.test_id = t.id
            JOIN lessons l ON t.lesson_id = l.id
            JOIN units u ON l.unit_id = u.id
            LEFT JOIN attempt_numbers an ON tr.id = an.id
            ORDER BY tr.started_at DESC
            LIMIT :limit OFFSET :offset
            """, Map.of("limit", limit, "offset", offset), RESULT_WITH_STUDENT_MAPPER);
    }

    public List<TestResultWithStudent> getTestResultsByStudentId(int studentId, int limit, int offset) {
        return jdbc.query("""
            WITH attempt_numbers AS (
                SELECT id, ROW_NUMBER() OVER (PARTITION BY student_id, test_id ORDER BY started_at ASC) as attempt_number
                FROM test_results
            )
            SELECT tr.id, tr.student_id, tr.test_id, tr.score, tr.max_score,
                   tr.started_at, tr.completed_at, tr.status,
                   s.full_name as student_name, t.title_uz as test_title,
                   l.title_uz as lesson_title, u.name as unit_name,
                   COALESCE(an.attempt_number, 1) as attempt_number
            FROM test_results tr
            JOIN students s ON tr.student_id = s.id
            JOIN tests t ON tr.test_id = t.id
            JOIN lessons l ON t.lesson_id = l.id
            JOIN units u ON l.unit_id = u.id
            LEFT JOIN attempt_numbers an ON tr.id = an.id
            WHERE tr.student_id = :studentId
            ORDER BY tr.started_at DESC
            LIMIT :limit OFFSET :offset
            """, Map.of("studentId", studentId, "limit", limit, "offset", offset), RESULT_WITH_STUDENT_MAPPER);
    }

    public int countTestResultsByStudentId(int studentId) {
        Integer count = jdbc.queryForObject(
            "SELECT COUNT(*) FROM test_results WHERE student_id = :studentId",
            Map.of("studentId", studentId),
            Integer.class);
        return count != null ? count : 0;
    }

    public int countTestResultsWithStudents() {
        Integer count = jdbc.queryForObject(
            "SELECT COUNT(*) FROM test_results tr JOIN students s ON tr.student_id = s.id",
            Map.of(), Integer.class);
        return count != null ? count : 0;
    }

    public List<TestResultWithStudent> getTestResultsFiltered(String groupName, String subgroupName,
                                                              String studentName, String testName,
                                                              String status, Integer unitId,
                                                              Integer lessonId, Integer testId,
                                                              int limit, int offset) {
        var conditions = new ArrayList<String>();
        var params = new MapSqlParameterSource();

        if (groupName != null && !groupName.isEmpty()) {
            conditions.add("LOWER(s.group_name) LIKE LOWER(:groupName)");
            params.addValue("groupName", "%" + groupName + "%");
        }
        if (subgroupName != null && !subgroupName.isEmpty()) {
            conditions.add("LOWER(s.subgroup) LIKE LOWER(:subgroupName)");
            params.addValue("subgroupName", "%" + subgroupName + "%");
        }
        if (studentName != null && !studentName.isEmpty()) {
            conditions.add("uz_translit(s.full_name) LIKE uz_translit(:studentName)");
            params.addValue("studentName", "%" + studentName + "%");
        }
        if (testName != null && !testName.isEmpty()) {
            conditions.add("(LOWER(t.title_uz) LIKE LOWER(:testName) OR LOWER(l.title_uz) LIKE LOWER(:testName) OR LOWER(u.name) LIKE LOWER(:testName))");
            params.addValue("testName", "%" + testName + "%");
        }
        if (status != null && !status.isEmpty()) {
            conditions.add("tr.status = :status");
            params.addValue("status", status);
        }
        if (unitId != null && unitId > 0) {
            conditions.add("u.id = :unitId");
            params.addValue("unitId", unitId);
        }
        if (lessonId != null && lessonId > 0) {
            conditions.add("l.id = :lessonId");
            params.addValue("lessonId", lessonId);
        }
        if (testId != null && testId > 0) {
            conditions.add("tr.test_id = :testId");
            params.addValue("testId", testId);
        }

        String where = conditions.isEmpty() ? "" : " WHERE " + String.join(" AND ", conditions);
        params.addValue("limit", limit).addValue("offset", offset);

        return jdbc.query("""
            WITH attempt_numbers AS (
                SELECT id, ROW_NUMBER() OVER (PARTITION BY student_id, test_id ORDER BY started_at ASC) as attempt_number
                FROM test_results
            )
            SELECT tr.id, tr.student_id, tr.test_id, tr.score, tr.max_score,
                   tr.started_at, tr.completed_at, tr.status,
                   s.full_name as student_name, t.title_uz as test_title,
                   l.title_uz as lesson_title, u.name as unit_name,
                   COALESCE(an.attempt_number, 1) as attempt_number
            FROM test_results tr
            JOIN students s ON tr.student_id = s.id
            JOIN tests t ON tr.test_id = t.id
            JOIN lessons l ON t.lesson_id = l.id
            JOIN units u ON l.unit_id = u.id
            LEFT JOIN attempt_numbers an ON tr.id = an.id
            """ + where + " ORDER BY tr.started_at DESC LIMIT :limit OFFSET :offset",
            params, RESULT_WITH_STUDENT_MAPPER);
    }

    public int countTestResultsFiltered(String groupName, String subgroupName, String studentName,
                                        String testName, String status, Integer unitId,
                                        Integer lessonId, Integer testId) {
        var conditions = new ArrayList<String>();
        var params = new MapSqlParameterSource();
        if (groupName != null && !groupName.isEmpty()) {
            conditions.add("LOWER(s.group_name) LIKE LOWER(:groupName)");
            params.addValue("groupName", "%" + groupName + "%");
        }
        if (subgroupName != null && !subgroupName.isEmpty()) {
            conditions.add("LOWER(s.subgroup) LIKE LOWER(:subgroupName)");
            params.addValue("subgroupName", "%" + subgroupName + "%");
        }
        if (studentName != null && !studentName.isEmpty()) {
            conditions.add("uz_translit(s.full_name) LIKE uz_translit(:studentName)");
            params.addValue("studentName", "%" + studentName + "%");
        }
        if (testName != null && !testName.isEmpty()) {
            conditions.add("(LOWER(t.title_uz) LIKE LOWER(:testName) OR LOWER(l.title_uz) LIKE LOWER(:testName) OR LOWER(u.name) LIKE LOWER(:testName))");
            params.addValue("testName", "%" + testName + "%");
        }
        if (status != null && !status.isEmpty()) {
            conditions.add("tr.status = :status");
            params.addValue("status", status);
        }
        if (unitId != null && unitId > 0) {
            conditions.add("u.id = :unitId");
            params.addValue("unitId", unitId);
        }
        if (lessonId != null && lessonId > 0) {
            conditions.add("l.id = :lessonId");
            params.addValue("lessonId", lessonId);
        }
        if (testId != null && testId > 0) {
            conditions.add("tr.test_id = :testId");
            params.addValue("testId", testId);
        }
        String where = conditions.isEmpty() ? "" : " WHERE " + String.join(" AND ", conditions);
        Integer count = jdbc.queryForObject("""
            SELECT COUNT(*)
            FROM test_results tr
            JOIN students s ON tr.student_id = s.id
            JOIN tests t ON tr.test_id = t.id
            JOIN lessons l ON t.lesson_id = l.id
            JOIN units u ON l.unit_id = u.id
            """ + where,
            params,
            Integer.class);
        return count != null ? count : 0;
    }

    public List<TestResultWithStudent> getTestResultsByGroupSubgroupAndTest(String groupName, String subgroup, int testId, int limit, int offset) {
        return jdbc.query("""
            WITH attempt_numbers AS (
                SELECT id, ROW_NUMBER() OVER (PARTITION BY student_id, test_id ORDER BY started_at ASC) as attempt_number
                FROM test_results
            )
            SELECT tr.id, tr.student_id, tr.test_id, tr.score, tr.max_score,
                   tr.started_at, tr.completed_at, tr.status,
                   s.full_name as student_name, t.title_uz as test_title,
                   l.title_uz as lesson_title, u.name as unit_name,
                   COALESCE(an.attempt_number, 1) as attempt_number
            FROM test_results tr
            JOIN students s ON tr.student_id = s.id
            JOIN tests t ON tr.test_id = t.id
            JOIN lessons l ON t.lesson_id = l.id
            JOIN units u ON l.unit_id = u.id
            LEFT JOIN attempt_numbers an ON tr.id = an.id
            WHERE tr.test_id = :testId
              AND LOWER(s.group_name) = LOWER(:groupName)
              AND LOWER(s.subgroup) = LOWER(:subgroup)
            ORDER BY s.full_name ASC, tr.started_at DESC
            LIMIT :limit OFFSET :offset
            """,
            Map.of("testId", testId, "groupName", groupName, "subgroup", subgroup,
                   "limit", limit, "offset", offset),
            RESULT_WITH_STUDENT_MAPPER);
    }

    public List<java.util.Map<String, Object>> getGroupStatsByTestId(int testId) {
        return jdbc.queryForList("""
            WITH best_per_student AS (
                SELECT DISTINCT ON (tr.student_id)
                    s.group_name,
                    s.subgroup,
                    s.full_name,
                    tr.score,
                    tr.max_score
                FROM test_results tr
                JOIN students s ON tr.student_id = s.id
                WHERE tr.test_id = :testId AND tr.status = 'completed'
                ORDER BY tr.student_id, tr.score DESC, tr.completed_at DESC
            )
            SELECT
                group_name,
                subgroup,
                COUNT(*) AS student_count,
                COUNT(CASE WHEN max_score > 0 AND score::float / max_score >= 0.6 THEN 1 END) AS passed_count,
                ROUND(AVG(score::float / NULLIF(max_score, 0) * 100)) AS avg_score_pct
            FROM best_per_student
            GROUP BY group_name, subgroup
            ORDER BY group_name, subgroup
            """, Map.of("testId", testId));
    }

    // ==================== TEST ANSWERS ====================

    public TestAnswer createTestAnswer(TestAnswer answer) {
        String sql = """
            INSERT INTO test_answers (result_id, question_id, selected_option_id, is_correct, answered_at)
            VALUES (:resultId, :questionId, :selectedOptionId, :isCorrect, :answeredAt)
            ON CONFLICT (result_id, question_id) DO UPDATE
            SET selected_option_id = :selectedOptionId, is_correct = :isCorrect, answered_at = :answeredAt
            RETURNING id
            """;
        var params = new MapSqlParameterSource()
            .addValue("resultId", answer.getResultId())
            .addValue("questionId", answer.getQuestionId())
            .addValue("selectedOptionId", answer.getSelectedOptionId())
            .addValue("isCorrect", answer.isCorrect())
            .addValue("answeredAt", answer.getAnsweredAt());
        var keyHolder = new GeneratedKeyHolder();
        jdbc.update(sql, params, keyHolder, new String[]{"id"});
        var keys = keyHolder.getKeys();
        if (keys != null) answer.setId((Integer) keys.get("id"));
        return answer;
    }

    public List<TestAnswer> getTestAnswersByResultId(int resultId) {
        return jdbc.query("""
            SELECT id, result_id, question_id, selected_option_id, is_correct, answered_at
            FROM test_answers WHERE result_id = :resultId ORDER BY answered_at
            """, Map.of("resultId", resultId), (rs, rowNum) -> {
            TestAnswer a = new TestAnswer();
            a.setId(rs.getInt("id"));
            a.setResultId(rs.getInt("result_id"));
            a.setQuestionId(rs.getInt("question_id"));
            int optId = rs.getInt("selected_option_id");
            a.setSelectedOptionId(rs.wasNull() ? null : optId);
            a.setCorrect(rs.getBoolean("is_correct"));
            a.setAnsweredAt(rs.getObject("answered_at", LocalDateTime.class));
            return a;
        });
    }

    public List<Map<String, Object>> getTestAnswerDetailsByResultId(int resultId) {
        return jdbc.query("""
            SELECT q.id as question_id,
                   q.order_num,
                   q.question_text,
                   q.points,
                   ta.selected_option_id,
                   selected.option_text as selected_option_text,
                   correct.id as correct_option_id,
                   correct.option_text as correct_option_text,
                   COALESCE(ta.is_correct, FALSE) as is_correct,
                   ta.answered_at
            FROM test_results tr
            JOIN questions q ON q.test_id = tr.test_id
            LEFT JOIN test_answers ta ON ta.result_id = tr.id AND ta.question_id = q.id
            LEFT JOIN answer_options selected ON selected.id = ta.selected_option_id
            LEFT JOIN answer_options correct ON correct.question_id = q.id AND correct.is_correct = TRUE
            WHERE tr.id = :resultId
            ORDER BY q.order_num ASC, correct.order_num ASC
            """, Map.of("resultId", resultId), (rs, rowNum) -> {
            Map<String, Object> row = new HashMap<>();
            row.put("questionId", rs.getInt("question_id"));
            row.put("orderNum", rs.getInt("order_num"));
            row.put("questionText", rs.getString("question_text"));
            row.put("points", rs.getInt("points"));
            int selectedOptionId = rs.getInt("selected_option_id");
            row.put("selectedOptionId", rs.wasNull() ? null : selectedOptionId);
            row.put("selectedOptionText", rs.getString("selected_option_text"));
            int correctOptionId = rs.getInt("correct_option_id");
            row.put("correctOptionId", rs.wasNull() ? null : correctOptionId);
            row.put("correctOptionText", rs.getString("correct_option_text"));
            row.put("isCorrect", rs.getBoolean("is_correct"));
            row.put("answeredAt", rs.getObject("answered_at", LocalDateTime.class));
            return row;
        });
    }

    public int calculateTestScore(int resultId) {
        Integer score = jdbc.queryForObject("""
            SELECT COALESCE(SUM(q.points), 0)
            FROM test_answers ta JOIN questions q ON ta.question_id = q.id
            WHERE ta.result_id = :resultId AND ta.is_correct = TRUE
            """, Map.of("resultId", resultId), Integer.class);
        return score != null ? score : 0;
    }

    // ==================== TEST RETAKES ====================

    public TestRetake createTestRetake(TestRetake retake) {
        String sql = """
            INSERT INTO test_retakes (student_id, test_id, granted_by)
            VALUES (:studentId, :testId, :grantedBy)
            RETURNING id, granted_at
            """;
        var params = new MapSqlParameterSource()
            .addValue("studentId", retake.getStudentId())
            .addValue("testId", retake.getTestId())
            .addValue("grantedBy", retake.getGrantedBy() > 0 ? retake.getGrantedBy() : null);
        var keyHolder = new GeneratedKeyHolder();
        jdbc.update(sql, params, keyHolder, new String[]{"id", "granted_at"});
        var keys = keyHolder.getKeys();
        if (keys != null) {
            retake.setId((Integer) keys.get("id"));
            retake.setGrantedAt(JdbcTimeUtils.toLocalDateTime(keys.get("granted_at")));
        }
        return retake;
    }

    // ==================== SITUATIONAL ANSWERS ====================

    public SituationalAnswer createSituationalAnswer(SituationalAnswer answer) {
        String sql = """
            INSERT INTO situational_answers (student_id, task_id, answer_text, photo_file_id, submitted_at)
            VALUES (:studentId, :taskId, :answerText, :photoFileId, :submittedAt)
            RETURNING id
            """;
        var params = new MapSqlParameterSource()
            .addValue("studentId", answer.getStudentId())
            .addValue("taskId", answer.getTaskId())
            .addValue("answerText", answer.getAnswerText())
            .addValue("photoFileId", answer.getPhotoFileId())
            .addValue("submittedAt", answer.getSubmittedAt());
        var keyHolder = new GeneratedKeyHolder();
        jdbc.update(sql, params, keyHolder, new String[]{"id"});
        var keys = keyHolder.getKeys();
        if (keys != null) answer.setId((Integer) keys.get("id"));
        return answer;
    }

    public SituationalAnswer getSituationalAnswerById(int id) {
        var results = jdbc.query("""
            SELECT id, student_id, task_id, answer_text, COALESCE(photo_file_id, ''), submitted_at,
                   is_graded, grade, COALESCE(feedback, ''), graded_by, graded_at, COALESCE(citations, '')
            FROM situational_answers WHERE id = :id
            """, Map.of("id", id), (rs, rowNum) -> {
            SituationalAnswer a = new SituationalAnswer();
            a.setId(rs.getInt(1));
            a.setStudentId(rs.getInt(2));
            a.setTaskId(rs.getInt(3));
            a.setAnswerText(rs.getString(4));
            a.setPhotoFileId(rs.getString(5));
            a.setSubmittedAt(rs.getObject(6, LocalDateTime.class));
            a.setGraded(rs.getBoolean(7));
            int grade = rs.getInt(8);
            a.setGrade(rs.wasNull() ? null : grade);
            a.setFeedback(rs.getString(9));
            int gradedBy = rs.getInt(10);
            a.setGradedBy(rs.wasNull() ? null : gradedBy);
            a.setGradedAt(rs.getObject(11, LocalDateTime.class));
            a.setCitations(rs.getString(12));
            return a;
        });
        return results.isEmpty() ? null : results.get(0);
    }

    public boolean hasSubmittedSituationalTask(int studentId, int taskId) {
        Boolean exists = jdbc.queryForObject(
            "SELECT EXISTS(SELECT 1 FROM situational_answers WHERE student_id = :studentId AND task_id = :taskId)",
            Map.of("studentId", studentId, "taskId", taskId), Boolean.class);
        return Boolean.TRUE.equals(exists);
    }

    public Map<Integer, Boolean> getSubmittedTaskIds(int studentId, List<Integer> taskIds) {
        if (taskIds == null || taskIds.isEmpty()) return new HashMap<>();
        var results = jdbc.queryForList(
            "SELECT task_id FROM situational_answers WHERE student_id = :studentId AND task_id IN (:taskIds)",
            Map.of("studentId", studentId, "taskIds", taskIds), Integer.class);
        Map<Integer, Boolean> map = new HashMap<>();
        results.forEach(id -> map.put(id, true));
        return map;
    }

    public void gradeSituationalAnswer(int id, int grade, String feedback, Integer gradedBy, String citations) {
        jdbc.update("""
            UPDATE situational_answers
            SET is_graded = TRUE, grade = :grade, feedback = :feedback,
                graded_by = :gradedBy, graded_at = :gradedAt, citations = :citations
            WHERE id = :id
            """,
            new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("grade", grade)
                .addValue("feedback", feedback)
                .addValue("gradedBy", gradedBy)
                .addValue("gradedAt", LocalDateTime.now())
                .addValue("citations", citations));
    }

    public List<SituationalAnswerWithStudent> getSituationalAnswersWithStudents(int limit, int offset) {
        return jdbc.query("""
            SELECT sa.id, sa.student_id, sa.task_id, sa.answer_text, COALESCE(sa.photo_file_id, '') as photo_file_id, sa.submitted_at,
                   sa.is_graded, sa.grade, COALESCE(sa.feedback, '') as feedback, sa.graded_by, sa.graded_at,
                   s.full_name as student_name,
                   COALESCE(st.task_text, 'Vazifa o''chirilgan') as task_text,
                   COALESCE(l.title_uz, 'Dars o''chirilgan') as lesson_title,
                   COALESCE(u.name, 'Bo''lim o''chirilgan') as unit_name
            FROM situational_answers sa
            JOIN students s ON sa.student_id = s.id
            LEFT JOIN situational_tasks st ON sa.task_id = st.id
            LEFT JOIN lessons l ON st.lesson_id = l.id
            LEFT JOIN units u ON l.unit_id = u.id
            ORDER BY sa.submitted_at DESC LIMIT :limit OFFSET :offset
            """, Map.of("limit", limit, "offset", offset), SIT_MAPPER);
    }

    public int countSituationalAnswersWithStudents() {
        Integer count = jdbc.queryForObject("""
            SELECT COUNT(*) FROM situational_answers sa
            JOIN students s ON sa.student_id = s.id
            LEFT JOIN situational_tasks st ON sa.task_id = st.id
            """, Map.of(), Integer.class);
        return count != null ? count : 0;
    }

    public int countSituationalAnswersFiltered(String groupName, String subgroupName, String studentName,
                                               String graded, Integer unitId, Integer lessonId, Integer taskId) {
        var conditions = new ArrayList<String>();
        var params = new MapSqlParameterSource();
        if (groupName != null && !groupName.isEmpty()) {
            conditions.add("LOWER(s.group_name) LIKE LOWER(:groupName)");
            params.addValue("groupName", "%" + groupName + "%");
        }
        if (subgroupName != null && !subgroupName.isEmpty()) {
            conditions.add("LOWER(s.subgroup) LIKE LOWER(:subgroupName)");
            params.addValue("subgroupName", "%" + subgroupName + "%");
        }
        if (studentName != null && !studentName.isEmpty()) {
            conditions.add("uz_translit(s.full_name) LIKE uz_translit(:studentName)");
            params.addValue("studentName", "%" + studentName + "%");
        }
        if (graded != null && !graded.isEmpty()) {
            conditions.add("sa.is_graded = :graded");
            params.addValue("graded", Boolean.parseBoolean(graded));
        }
        if (unitId != null && unitId > 0) {
            conditions.add("u.id = :unitId");
            params.addValue("unitId", unitId);
        }
        if (lessonId != null && lessonId > 0) {
            conditions.add("l.id = :lessonId");
            params.addValue("lessonId", lessonId);
        }
        if (taskId != null && taskId > 0) {
            conditions.add("sa.task_id = :taskId");
            params.addValue("taskId", taskId);
        }
        String where = conditions.isEmpty() ? "" : " WHERE " + String.join(" AND ", conditions);
        Integer count = jdbc.queryForObject("""
            SELECT COUNT(*)
            FROM situational_answers sa
            JOIN students s ON sa.student_id = s.id
            LEFT JOIN situational_tasks st ON sa.task_id = st.id
            LEFT JOIN lessons l ON st.lesson_id = l.id
            LEFT JOIN units u ON l.unit_id = u.id
            """ + where,
            params,
            Integer.class);
        return count != null ? count : 0;
    }

    public List<SituationalAnswerWithStudent> getSituationalAnswersFiltered(String groupName, String subgroupName,
                                                                            String studentName, String graded,
                                                                            Integer unitId, Integer lessonId,
                                                                            Integer taskId, int limit, int offset) {
        var conditions = new ArrayList<String>();
        var params = new MapSqlParameterSource();
        if (groupName != null && !groupName.isEmpty()) {
            conditions.add("LOWER(s.group_name) LIKE LOWER(:groupName)");
            params.addValue("groupName", "%" + groupName + "%");
        }
        if (subgroupName != null && !subgroupName.isEmpty()) {
            conditions.add("LOWER(s.subgroup) LIKE LOWER(:subgroupName)");
            params.addValue("subgroupName", "%" + subgroupName + "%");
        }
        if (studentName != null && !studentName.isEmpty()) {
            conditions.add("uz_translit(s.full_name) LIKE uz_translit(:studentName)");
            params.addValue("studentName", "%" + studentName + "%");
        }
        if (graded != null && !graded.isEmpty()) {
            conditions.add("sa.is_graded = :graded");
            params.addValue("graded", Boolean.parseBoolean(graded));
        }
        if (unitId != null && unitId > 0) {
            conditions.add("u.id = :unitId");
            params.addValue("unitId", unitId);
        }
        if (lessonId != null && lessonId > 0) {
            conditions.add("l.id = :lessonId");
            params.addValue("lessonId", lessonId);
        }
        if (taskId != null && taskId > 0) {
            conditions.add("sa.task_id = :taskId");
            params.addValue("taskId", taskId);
        }
        String where = conditions.isEmpty() ? "" : " WHERE " + String.join(" AND ", conditions);
        params.addValue("limit", limit).addValue("offset", offset);

        return jdbc.query("""
            SELECT sa.id, sa.student_id, sa.task_id, sa.answer_text, COALESCE(sa.photo_file_id, '') as photo_file_id, sa.submitted_at,
                   sa.is_graded, sa.grade, COALESCE(sa.feedback, '') as feedback, sa.graded_by, sa.graded_at,
                   s.full_name as student_name,
                   COALESCE(st.task_text, 'Vazifa o''chirilgan') as task_text,
                   COALESCE(l.title_uz, 'Dars o''chirilgan') as lesson_title,
                   COALESCE(u.name, 'Bo''lim o''chirilgan') as unit_name
            FROM situational_answers sa
            JOIN students s ON sa.student_id = s.id
            LEFT JOIN situational_tasks st ON sa.task_id = st.id
            LEFT JOIN lessons l ON st.lesson_id = l.id
            LEFT JOIN units u ON l.unit_id = u.id
            """ + where + " ORDER BY sa.submitted_at DESC LIMIT :limit OFFSET :offset",
            params, SIT_MAPPER);
    }

    public boolean hasAvailableSituationalRetake(int studentId, int taskId) {
        Boolean exists = jdbc.queryForObject("""
            SELECT EXISTS(SELECT 1 FROM situational_retakes WHERE student_id = :studentId AND task_id = :taskId AND used = FALSE)
            """, Map.of("studentId", studentId, "taskId", taskId), Boolean.class);
        return Boolean.TRUE.equals(exists);
    }

    public SituationalRetake createSituationalRetake(SituationalRetake retake) {
        String sql = """
            INSERT INTO situational_retakes (student_id, task_id, granted_by)
            VALUES (:studentId, :taskId, :grantedBy)
            ON CONFLICT (student_id, task_id) DO UPDATE SET used = FALSE, used_at = NULL, granted_at = NOW(), granted_by = :grantedBy
            RETURNING id, granted_at
            """;
        var params = new MapSqlParameterSource()
            .addValue("studentId", retake.getStudentId())
            .addValue("taskId", retake.getTaskId())
            .addValue("grantedBy", retake.getGrantedBy() > 0 ? retake.getGrantedBy() : null);
        var keyHolder = new GeneratedKeyHolder();
        jdbc.update(sql, params, keyHolder, new String[]{"id", "granted_at"});
        var keys = keyHolder.getKeys();
        if (keys != null) {
            retake.setId((Integer) keys.get("id"));
            retake.setGrantedAt(JdbcTimeUtils.toLocalDateTime(keys.get("granted_at")));
        }
        return retake;
    }

    // ==================== ATOMIC TRANSACTIONS ====================

    public TestResult startTestAtomically(int studentId, int testId, boolean isRetake, int maxScore) {
        return tx.execute(status -> {
            if (isRetake) {
                // Lock the unused retake row — FOR UPDATE prevents concurrent consumption
                var retakeIds = jdbc.queryForList("""
                    SELECT id FROM test_retakes
                    WHERE student_id = :studentId AND test_id = :testId AND used = FALSE
                    ORDER BY granted_at DESC LIMIT 1
                    FOR UPDATE
                    """, Map.of("studentId", studentId, "testId", testId), Integer.class);
                if (retakeIds.isEmpty()) {
                    throw new IllegalStateException("No available retake for student=" + studentId + " test=" + testId);
                }
                int retakeId = retakeIds.get(0);
                jdbc.update("UPDATE test_retakes SET used = TRUE, used_at = :now WHERE id = :id",
                    Map.of("now", LocalDateTime.now(), "id", retakeId));
            }

            // Clean up orphaned in_progress rows
            jdbc.update("""
                UPDATE test_results SET status = 'timeout', completed_at = NOW()
                WHERE student_id = :studentId AND test_id = :testId AND status = 'in_progress'
                """, Map.of("studentId", studentId, "testId", testId));

            // Create the new result row
            String insertSql = """
                INSERT INTO test_results (student_id, test_id, score, max_score, started_at, status)
                VALUES (:studentId, :testId, 0, :maxScore, :startedAt, 'in_progress')
                RETURNING id
                """;
            var params = new MapSqlParameterSource()
                .addValue("studentId", studentId)
                .addValue("testId", testId)
                .addValue("maxScore", maxScore)
                .addValue("startedAt", LocalDateTime.now());
            var keyHolder = new GeneratedKeyHolder();
            jdbc.update(insertSql, params, keyHolder, new String[]{"id"});
            var keys = keyHolder.getKeys();

            TestResult result = new TestResult();
            result.setStudentId(studentId);
            result.setTestId(testId);
            result.setMaxScore(maxScore);
            result.setStatus("in_progress");
            result.setStartedAt(LocalDateTime.now());
            if (keys != null) result.setId((Integer) keys.get("id"));
            return result;
        });
    }

    public int forceCompleteExpiredTestResults() {
        return jdbc.getJdbcOperations().update("""
            UPDATE test_results
            SET status = 'timeout', completed_at = NOW()
            WHERE status = 'in_progress'
              AND id IN (
                SELECT tr.id FROM test_results tr
                JOIN tests t ON tr.test_id = t.id
                WHERE tr.status = 'in_progress'
                  AND tr.started_at + (t.time_limit_minutes * interval '1 minute') < NOW()
              )
            """);
    }

    public void useSituationalRetakeAtomically(int studentId, int taskId) {
        tx.executeWithoutResult(status -> {
            // Lock the unused retake row
            var retakeIds = jdbc.queryForList("""
                SELECT id FROM situational_retakes
                WHERE student_id = :studentId AND task_id = :taskId AND used = FALSE
                FOR UPDATE
                """, Map.of("studentId", studentId, "taskId", taskId), Integer.class);
            if (retakeIds.isEmpty()) {
                throw new IllegalStateException("No available situational retake for student=" + studentId + " task=" + taskId);
            }
            int retakeId = retakeIds.get(0);

            // Delete old answer
            jdbc.update("DELETE FROM situational_answers WHERE student_id = :studentId AND task_id = :taskId",
                Map.of("studentId", studentId, "taskId", taskId));

            // Mark retake as used
            jdbc.update("UPDATE situational_retakes SET used = TRUE, used_at = :now WHERE id = :id",
                Map.of("now", LocalDateTime.now(), "id", retakeId));
        });
    }
}
