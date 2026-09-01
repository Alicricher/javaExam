package com.dentistrybot.shared.repository;

import com.dentistrybot.shared.model.*;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Repository
public class TestRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public TestRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Used by AccessControlService to resolve which unit ("predmet") a test belongs to. */
    public Integer getUnitIdForTest(int testId) {
        return jdbc.query("""
            SELECT l.unit_id FROM tests t JOIN lessons l ON t.lesson_id = l.id WHERE t.id = :testId
            """, Map.of("testId", testId), (rs, rn) -> rs.getInt("unit_id"))
            .stream().findFirst().orElse(null);
    }

    /** Used by AccessControlService to resolve which unit ("predmet") a question belongs to. */
    public Integer getUnitIdForQuestion(int questionId) {
        return jdbc.query("""
            SELECT l.unit_id FROM questions q
            JOIN tests t ON q.test_id = t.id
            JOIN lessons l ON t.lesson_id = l.id
            WHERE q.id = :questionId
            """, Map.of("questionId", questionId), (rs, rn) -> rs.getInt("unit_id"))
            .stream().findFirst().orElse(null);
    }

    private static final RowMapper<Test> TEST_MAPPER = (rs, rowNum) -> {
        Test t = new Test();
        t.setId(rs.getInt("id"));
        t.setLessonId(rs.getInt("lesson_id"));
        t.setTitleUz(rs.getString("title_uz"));
        t.setTitleRu(rs.getString("title_ru"));
        t.setTimeLimitMinutes(rs.getInt("time_limit_minutes"));
        t.setTotalPoints(rs.getInt("total_points"));
        t.setActive(rs.getBoolean("is_active"));
        t.setCreatedAt(rs.getObject("created_at", LocalDateTime.class));
        return t;
    };

    private static final RowMapper<Question> QUESTION_MAPPER = (rs, rowNum) -> {
        Question q = new Question();
        q.setId(rs.getInt("id"));
        q.setTestId(rs.getInt("test_id"));
        q.setQuestionText(rs.getString("question_text"));
        q.setQuestionTextRu(rs.getString("question_text_ru"));
        q.setPoints(rs.getInt("points"));
        q.setOrderNum(rs.getInt("order_num"));
        q.setPhotoFilePath(rs.getString("photo_file_path"));
        q.setCreatedAt(rs.getObject("created_at", LocalDateTime.class));
        return q;
    };

    private static final RowMapper<AnswerOption> OPTION_MAPPER = (rs, rowNum) -> {
        AnswerOption o = new AnswerOption();
        o.setId(rs.getInt("id"));
        o.setQuestionId(rs.getInt("question_id"));
        o.setOptionText(rs.getString("option_text"));
        o.setOptionTextRu(rs.getString("option_text_ru"));
        o.setCorrect(rs.getBoolean("is_correct"));
        o.setOrderNum(rs.getInt("order_num"));
        return o;
    };

    // ==================== TESTS ====================

    public Test getTestByLessonId(int lessonId) {
        String sql = """
            SELECT id, lesson_id, title_uz, title_ru, time_limit_minutes, total_points, is_active, created_at
            FROM tests WHERE lesson_id = :lessonId AND is_active = TRUE LIMIT 1
            """;
        var results = jdbc.query(sql, Map.of("lessonId", lessonId), TEST_MAPPER);
        return results.isEmpty() ? null : results.get(0);
    }

    public Test getTestById(int id) {
        String sql = """
            SELECT id, lesson_id, title_uz, title_ru, time_limit_minutes, total_points, is_active, created_at
            FROM tests WHERE id = :id
            """;
        var results = jdbc.query(sql, Map.of("id", id), TEST_MAPPER);
        return results.isEmpty() ? null : results.get(0);
    }

    public Test createTest(Test test) {
        String sql = """
            INSERT INTO tests (lesson_id, title_uz, time_limit_minutes, total_points)
            VALUES (:lessonId, :titleUz, :timeLimitMinutes, :totalPoints)
            RETURNING id, created_at
            """;
        var params = new MapSqlParameterSource()
            .addValue("lessonId", test.getLessonId())
            .addValue("titleUz", test.getTitleUz())
            .addValue("timeLimitMinutes", test.getTimeLimitMinutes())
            .addValue("totalPoints", test.getTotalPoints());
        var keyHolder = new GeneratedKeyHolder();
        jdbc.update(sql, params, keyHolder, new String[]{"id", "created_at"});
        var keys = keyHolder.getKeys();
        if (keys != null) {
            test.setId((Integer) keys.get("id"));
            test.setCreatedAt(JdbcTimeUtils.toLocalDateTime(keys.get("created_at")));
        }
        return test;
    }

    public void updateTest(Test test) {
        jdbc.update("""
            UPDATE tests SET title_uz = :titleUz, time_limit_minutes = :timeLimitMinutes, total_points = :totalPoints WHERE id = :id
            """,
            Map.of("id", test.getId(), "titleUz", test.getTitleUz(),
                   "timeLimitMinutes", test.getTimeLimitMinutes(), "totalPoints", test.getTotalPoints()));
    }

    public void deleteTest(int id) {
        jdbc.update("UPDATE tests SET is_active = FALSE WHERE id = :id", Map.of("id", id));
    }

    public void updateTestTotalPoints(int testId) {
        jdbc.update("""
            UPDATE tests SET total_points = (SELECT COALESCE(SUM(points), 0) FROM questions WHERE test_id = :testId)
            WHERE id = :testId
            """, Map.of("testId", testId));
    }

    // ==================== QUESTIONS ====================

    public List<Question> getQuestionsByTestId(int testId) {
        String sql = """
            SELECT id, test_id, question_text, question_text_ru, points, order_num, photo_file_path, created_at
            FROM questions WHERE test_id = :testId ORDER BY order_num
            """;
        return jdbc.query(sql, Map.of("testId", testId), QUESTION_MAPPER);
    }

    public Question getQuestionById(int id) {
        var results = jdbc.query("""
            SELECT id, test_id, question_text, question_text_ru, points, order_num, photo_file_path, created_at FROM questions WHERE id = :id
            """, Map.of("id", id), QUESTION_MAPPER);
        return results.isEmpty() ? null : results.get(0);
    }

    public List<Question> getQuestionsByTestIdPaginated(int testId, int limit, int offset) {
        String sql = """
            SELECT id, test_id, question_text, question_text_ru, points, order_num, photo_file_path, created_at
            FROM questions WHERE test_id = :testId ORDER BY order_num LIMIT :limit OFFSET :offset
            """;
        return jdbc.query(sql,
            Map.of("testId", testId, "limit", limit, "offset", offset), QUESTION_MAPPER);
    }

    public int countQuestions(int testId) {
        Integer count = jdbc.queryForObject(
            "SELECT COUNT(*) FROM questions WHERE test_id = :testId",
            Map.of("testId", testId), Integer.class);
        return count != null ? count : 0;
    }

    public QuestionWithOptions getQuestionWithOptions(int questionId) {
        Question q = getQuestionById(questionId);
        if (q == null) return null;
        List<AnswerOption> options = getAnswerOptionsByQuestionId(questionId);
        return new QuestionWithOptions(q, options);
    }

    public Question createQuestion(Question question) {
        String sql = """
            INSERT INTO questions (test_id, question_text, points, order_num)
            VALUES (:testId, :questionText, :points, :orderNum)
            RETURNING id, created_at
            """;
        var params = new MapSqlParameterSource()
            .addValue("testId", question.getTestId())
            .addValue("questionText", question.getQuestionText())
            .addValue("points", question.getPoints())
            .addValue("orderNum", question.getOrderNum());
        var keyHolder = new GeneratedKeyHolder();
        jdbc.update(sql, params, keyHolder, new String[]{"id", "created_at"});
        var keys = keyHolder.getKeys();
        if (keys != null) {
            question.setId((Integer) keys.get("id"));
            question.setCreatedAt(JdbcTimeUtils.toLocalDateTime(keys.get("created_at")));
        }
        updateTestTotalPoints(question.getTestId());
        return question;
    }

    public void updateQuestion(Question question) {
        jdbc.update("UPDATE questions SET question_text = :text, points = :points WHERE id = :id",
            Map.of("id", question.getId(), "text", question.getQuestionText(), "points", question.getPoints()));
        updateTestTotalPoints(question.getTestId());
    }

    public void updateQuestionPhotoFilePath(int id, String photoFilePath) {
        jdbc.update("UPDATE questions SET photo_file_path = :photoFilePath WHERE id = :id",
            Map.of("id", id, "photoFilePath", photoFilePath));
    }

    public void deleteQuestion(int id, int testId) {
        jdbc.update("DELETE FROM questions WHERE id = :id", Map.of("id", id));
        updateTestTotalPoints(testId);
    }

    public void deleteAllQuestions(int testId) {
        jdbc.update("DELETE FROM questions WHERE test_id = :testId", Map.of("testId", testId));
        updateTestTotalPoints(testId);
    }

    public int getNextQuestionOrderNum(int testId) {
        Integer max = jdbc.queryForObject(
            "SELECT COALESCE(MAX(order_num), 0) FROM questions WHERE test_id = :testId",
            Map.of("testId", testId), Integer.class);
        return (max != null ? max : 0) + 1;
    }

    public void swapQuestionOrders(int question1Id, int question2Id) {
        Integer order1 = jdbc.queryForObject("SELECT order_num FROM questions WHERE id = :id",
            Map.of("id", question1Id), Integer.class);
        Integer order2 = jdbc.queryForObject("SELECT order_num FROM questions WHERE id = :id",
            Map.of("id", question2Id), Integer.class);
        if (order1 == null || order2 == null) return;
        jdbc.update("UPDATE questions SET order_num = :order WHERE id = :id",
            Map.of("id", question1Id, "order", order2));
        jdbc.update("UPDATE questions SET order_num = :order WHERE id = :id",
            Map.of("id", question2Id, "order", order1));
    }

    // ==================== ANSWER OPTIONS ====================

    public List<AnswerOption> getAnswerOptionsByQuestionId(int questionId) {
        String sql = """
            SELECT id, question_id, option_text, option_text_ru, is_correct, order_num
            FROM answer_options WHERE question_id = :questionId ORDER BY order_num
            """;
        return jdbc.query(sql, Map.of("questionId", questionId), OPTION_MAPPER);
    }

    public AnswerOption getAnswerOptionById(int id) {
        var results = jdbc.query("""
            SELECT id, question_id, option_text, option_text_ru, is_correct, order_num FROM answer_options WHERE id = :id
            """, Map.of("id", id), OPTION_MAPPER);
        return results.isEmpty() ? null : results.get(0);
    }

    public AnswerOption createAnswerOption(AnswerOption option) {
        String sql = """
            INSERT INTO answer_options (question_id, option_text, is_correct, order_num)
            VALUES (:questionId, :optionText, :isCorrect, :orderNum)
            RETURNING id
            """;
        var params = new MapSqlParameterSource()
            .addValue("questionId", option.getQuestionId())
            .addValue("optionText", option.getOptionText())
            .addValue("isCorrect", option.isCorrect())
            .addValue("orderNum", option.getOrderNum());
        var keyHolder = new GeneratedKeyHolder();
        jdbc.update(sql, params, keyHolder, new String[]{"id"});
        var keys = keyHolder.getKeys();
        if (keys != null) option.setId((Integer) keys.get("id"));
        return option;
    }

    public void updateAnswerOption(AnswerOption option) {
        jdbc.update("UPDATE answer_options SET option_text = :text, is_correct = :isCorrect WHERE id = :id",
            Map.of("id", option.getId(), "text", option.getOptionText(), "isCorrect", option.isCorrect()));
    }

    public void deleteAnswerOption(int id) {
        jdbc.update("DELETE FROM answer_options WHERE id = :id", Map.of("id", id));
    }

    public void setCorrectAnswer(int questionId, int correctOptionId) {
        jdbc.update("UPDATE answer_options SET is_correct = FALSE WHERE question_id = :questionId",
            Map.of("questionId", questionId));
        jdbc.update("UPDATE answer_options SET is_correct = TRUE WHERE id = :id AND question_id = :questionId",
            Map.of("id", correctOptionId, "questionId", questionId));
    }
}
