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
public class LessonRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public LessonRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final RowMapper<Unit> UNIT_MAPPER = (rs, rowNum) -> {
        Unit u = new Unit();
        u.setId(rs.getInt("id"));
        u.setName(rs.getString("name"));
        u.setTitleUz(rs.getString("title_uz"));
        u.setCreatedAt(rs.getObject("created_at", LocalDateTime.class));
        return u;
    };

    private static final RowMapper<Lesson> LESSON_MAPPER = (rs, rowNum) -> {
        Lesson l = new Lesson();
        l.setId(rs.getInt("id"));
        l.setUnitId(rs.getInt("unit_id"));
        l.setLessonNumber(rs.getInt("lesson_number"));
        l.setTitleUz(rs.getString("title_uz"));
        l.setCreatedAt(rs.getObject("created_at", LocalDateTime.class));
        return l;
    };

    private static final RowMapper<TheoryMaterial> THEORY_MAPPER = (rs, rowNum) -> {
        TheoryMaterial m = new TheoryMaterial();
        m.setId(rs.getInt("id"));
        m.setLessonId(rs.getInt("lesson_id"));
        m.setTitleUz(rs.getString("title_uz"));
        m.setMaterialType(rs.getString("material_type"));
        m.setFilePath(rs.getString("file_path"));
        m.setDescription(rs.getString("description"));
        m.setCreatedAt(rs.getObject("created_at", LocalDateTime.class));
        return m;
    };

    private static final RowMapper<SituationalTask> TASK_MAPPER = (rs, rowNum) -> {
        SituationalTask t = new SituationalTask();
        t.setId(rs.getInt("id"));
        t.setLessonId(rs.getInt("lesson_id"));
        t.setOrderNum(rs.getInt("order_num"));
        t.setTaskText(rs.getString("task_text"));
        t.setTimeLimitMinutes(rs.getInt("time_limit_minutes"));
        t.setActive(rs.getBoolean("is_active"));
        t.setCreatedAt(rs.getObject("created_at", LocalDateTime.class));
        return t;
    };

    // ==================== UNITS ====================

    public List<Unit> getAllUnits() {
        String sql = """
            SELECT id, name, title_uz, created_at FROM units
            ORDER BY SUBSTRING(name FROM '^[A-Za-z]+'), CAST(SUBSTRING(name FROM '[0-9]+') AS INTEGER)
            """;
        return jdbc.query(sql, Map.of(), UNIT_MAPPER);
    }

    public Unit getUnitById(int id) {
        var results = jdbc.query("SELECT id, name, title_uz, created_at FROM units WHERE id = :id",
            Map.of("id", id), UNIT_MAPPER);
        return results.isEmpty() ? null : results.get(0);
    }

    public Unit getUnitByName(String name) {
        var results = jdbc.query("SELECT id, name, title_uz, created_at FROM units WHERE name = :name",
            Map.of("name", name), UNIT_MAPPER);
        return results.isEmpty() ? null : results.get(0);
    }

    public Unit createUnit(Unit unit) {
        String sql = "INSERT INTO units (name, title_uz) VALUES (:name, :titleUz) RETURNING id, created_at";
        var params = new MapSqlParameterSource()
            .addValue("name", unit.getName())
            .addValue("titleUz", unit.getTitleUz());
        var keyHolder = new GeneratedKeyHolder();
        jdbc.update(sql, params, keyHolder, new String[]{"id", "created_at"});
        var keys = keyHolder.getKeys();
        if (keys != null) {
            unit.setId((Integer) keys.get("id"));
            unit.setCreatedAt((LocalDateTime) keys.get("created_at"));
        }
        return unit;
    }

    public void updateUnit(Unit unit) {
        jdbc.update("UPDATE units SET name = :name, title_uz = :titleUz WHERE id = :id",
            Map.of("id", unit.getId(), "name", unit.getName(), "titleUz", unit.getTitleUz()));
    }

    public void deleteUnit(int id) {
        jdbc.update("DELETE FROM units WHERE id = :id", Map.of("id", id));
    }

    public boolean checkUnitNameExists(String name, int excludeId) {
        Boolean exists = jdbc.queryForObject(
            "SELECT EXISTS(SELECT 1 FROM units WHERE name = :name AND id != :excludeId)",
            Map.of("name", name, "excludeId", excludeId), Boolean.class);
        return Boolean.TRUE.equals(exists);
    }

    // ==================== LESSONS ====================

    public List<Lesson> getLessonsByUnitId(int unitId) {
        String sql = """
            SELECT id, unit_id, lesson_number, title_uz, created_at
            FROM lessons WHERE unit_id = :unitId ORDER BY lesson_number
            """;
        return jdbc.query(sql, Map.of("unitId", unitId), LESSON_MAPPER);
    }

    public Lesson getLessonById(int id) {
        var results = jdbc.query(
            "SELECT id, unit_id, lesson_number, title_uz, created_at FROM lessons WHERE id = :id",
            Map.of("id", id), LESSON_MAPPER);
        return results.isEmpty() ? null : results.get(0);
    }

    public Lesson createLesson(Lesson lesson) {
        String sql = """
            INSERT INTO lessons (unit_id, lesson_number, title_uz)
            VALUES (:unitId, :lessonNumber, :titleUz) RETURNING id, created_at
            """;
        var params = new MapSqlParameterSource()
            .addValue("unitId", lesson.getUnitId())
            .addValue("lessonNumber", lesson.getLessonNumber())
            .addValue("titleUz", lesson.getTitleUz());
        var keyHolder = new GeneratedKeyHolder();
        jdbc.update(sql, params, keyHolder, new String[]{"id", "created_at"});
        var keys = keyHolder.getKeys();
        if (keys != null) {
            lesson.setId((Integer) keys.get("id"));
            lesson.setCreatedAt((LocalDateTime) keys.get("created_at"));
        }
        return lesson;
    }

    public void updateLesson(Lesson lesson) {
        jdbc.update("UPDATE lessons SET title_uz = :titleUz WHERE id = :id",
            Map.of("id", lesson.getId(), "titleUz", lesson.getTitleUz()));
    }

    public void deleteLesson(int id) {
        jdbc.update("DELETE FROM lessons WHERE id = :id", Map.of("id", id));
    }

    public void renumberLessons(int unitId) {
        List<Lesson> lessons = getLessonsByUnitId(unitId);
        for (int i = 0; i < lessons.size(); i++) {
            int newNumber = i + 1;
            if (lessons.get(i).getLessonNumber() != newNumber) {
                jdbc.update("UPDATE lessons SET lesson_number = :num WHERE id = :id",
                    Map.of("num", newNumber, "id", lessons.get(i).getId()));
            }
        }
    }

    // ==================== THEORY MATERIALS ====================

    public List<TheoryMaterial> getTheoryMaterialsByLessonId(int lessonId) {
        String sql = """
            SELECT id, lesson_id, title_uz, material_type,
                   COALESCE(file_path, '') as file_path,
                   COALESCE(description, '') as description,
                   created_at
            FROM theory_materials WHERE lesson_id = :lessonId ORDER BY material_type, title_uz
            """;
        return jdbc.query(sql, Map.of("lessonId", lessonId), THEORY_MAPPER);
    }

    public TheoryMaterial getTheoryMaterialById(int id) {
        String sql = """
            SELECT id, lesson_id, title_uz, material_type,
                   COALESCE(file_path, '') as file_path,
                   COALESCE(description, '') as description,
                   created_at
            FROM theory_materials WHERE id = :id
            """;
        var results = jdbc.query(sql, Map.of("id", id), THEORY_MAPPER);
        return results.isEmpty() ? null : results.get(0);
    }

    public TheoryMaterial createTheoryMaterial(TheoryMaterial material) {
        String sql = """
            INSERT INTO theory_materials (lesson_id, title_uz, material_type, file_path, description)
            VALUES (:lessonId, :titleUz, :materialType, :filePath, :description)
            RETURNING id, created_at
            """;
        var params = new MapSqlParameterSource()
            .addValue("lessonId", material.getLessonId())
            .addValue("titleUz", material.getTitleUz())
            .addValue("materialType", material.getMaterialType())
            .addValue("filePath", material.getFilePath())
            .addValue("description", material.getDescription());
        var keyHolder = new GeneratedKeyHolder();
        jdbc.update(sql, params, keyHolder, new String[]{"id", "created_at"});
        var keys = keyHolder.getKeys();
        if (keys != null) {
            material.setId((Integer) keys.get("id"));
            material.setCreatedAt((LocalDateTime) keys.get("created_at"));
        }
        return material;
    }

    public void deleteTheoryMaterial(int id) {
        jdbc.update("DELETE FROM theory_materials WHERE id = :id", Map.of("id", id));
    }

    public void updateTheoryMaterialTitle(int id, String title) {
        jdbc.update("UPDATE theory_materials SET title_uz = :title WHERE id = :id",
            Map.of("id", id, "title", title));
    }

    public void updateTheoryMaterialDescription(int id, String description) {
        jdbc.update("UPDATE theory_materials SET description = :description WHERE id = :id",
            Map.of("id", id, "description", description));
    }

    public void updateTheoryMaterialType(int id, String materialType) {
        jdbc.update("UPDATE theory_materials SET material_type = :materialType WHERE id = :id",
            Map.of("id", id, "materialType", materialType));
    }

    public void updateTheoryMaterialFilePath(int id, String filePath) {
        jdbc.update("UPDATE theory_materials SET file_path = :filePath WHERE id = :id",
            Map.of("id", id, "filePath", filePath));
    }

    // ==================== SITUATIONAL TASKS ====================

    public List<SituationalTask> getSituationalTasksByLessonId(int lessonId) {
        String sql = """
            SELECT id, lesson_id, order_num, task_text, time_limit_minutes, is_active, created_at
            FROM situational_tasks WHERE lesson_id = :lessonId AND is_active = TRUE ORDER BY order_num
            """;
        return jdbc.query(sql, Map.of("lessonId", lessonId), TASK_MAPPER);
    }

    public SituationalTask getSituationalTaskById(int id) {
        String sql = """
            SELECT id, lesson_id, order_num, task_text, time_limit_minutes, is_active, created_at
            FROM situational_tasks WHERE id = :id
            """;
        var results = jdbc.query(sql, Map.of("id", id), TASK_MAPPER);
        return results.isEmpty() ? null : results.get(0);
    }

    public int getNextTaskOrderNum(int lessonId) {
        Integer max = jdbc.queryForObject(
            "SELECT COALESCE(MAX(order_num), 0) FROM situational_tasks WHERE lesson_id = :lessonId AND is_active = TRUE",
            Map.of("lessonId", lessonId), Integer.class);
        return (max != null ? max : 0) + 1;
    }

    public SituationalTask createSituationalTask(SituationalTask task) {
        String sql = """
            INSERT INTO situational_tasks (lesson_id, task_text, time_limit_minutes, order_num)
            VALUES (:lessonId, :taskText, :timeLimitMinutes, :orderNum)
            RETURNING id, created_at
            """;
        var params = new MapSqlParameterSource()
            .addValue("lessonId", task.getLessonId())
            .addValue("taskText", task.getTaskText())
            .addValue("timeLimitMinutes", task.getTimeLimitMinutes())
            .addValue("orderNum", task.getOrderNum());
        var keyHolder = new GeneratedKeyHolder();
        jdbc.update(sql, params, keyHolder, new String[]{"id", "created_at"});
        var keys = keyHolder.getKeys();
        if (keys != null) {
            task.setId((Integer) keys.get("id"));
            task.setCreatedAt((LocalDateTime) keys.get("created_at"));
        }
        return task;
    }

    public void updateSituationalTaskTime(int taskId, int timeLimit) {
        jdbc.update("UPDATE situational_tasks SET time_limit_minutes = :timeLimit WHERE id = :id",
            Map.of("id", taskId, "timeLimit", timeLimit));
    }

    public void updateSituationalTaskText(int taskId, String text) {
        jdbc.update("UPDATE situational_tasks SET task_text = :text WHERE id = :id",
            Map.of("id", taskId, "text", text));
    }

    public void deleteSituationalTask(int id) {
        jdbc.update("UPDATE situational_tasks SET is_active = FALSE WHERE id = :id", Map.of("id", id));
    }

    public void renumberSituationalTasks(int lessonId) {
        jdbc.update("""
            UPDATE situational_tasks t
            SET order_num = sub.rn
            FROM (
                SELECT id, ROW_NUMBER() OVER (ORDER BY order_num) AS rn
                FROM situational_tasks
                WHERE lesson_id = :lessonId AND is_active = TRUE
            ) sub
            WHERE t.id = sub.id AND t.lesson_id = :lessonId
            """, Map.of("lessonId", lessonId));
    }
}
