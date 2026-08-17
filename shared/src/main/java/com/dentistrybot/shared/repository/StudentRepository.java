package com.dentistrybot.shared.repository;

import com.dentistrybot.shared.model.Student;
import com.dentistrybot.shared.model.StudentFilter;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Repository
public class StudentRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public StudentRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final RowMapper<Student> STUDENT_MAPPER = (rs, rowNum) -> {
        Student s = new Student();
        s.setId(rs.getInt("id"));
        s.setTelegramId(rs.getLong("telegram_id"));
        s.setFullName(rs.getString("full_name"));
        s.setCourse(rs.getInt("course"));
        s.setGroupName(rs.getString("group_name"));
        s.setSubgroup(rs.getString("subgroup"));
        s.setFaculty(rs.getString("faculty"));
        s.setLanguage(rs.getString("language"));
        s.setCreatedAt(rs.getObject("created_at", LocalDateTime.class));
        s.setUpdatedAt(rs.getObject("updated_at", LocalDateTime.class));
        return s;
    };

    public Student createStudent(Student student) {
        String sql = """
            INSERT INTO students (telegram_id, full_name, course, group_name, subgroup, faculty, language)
            VALUES (:telegramId, :fullName, :course, :groupName, :subgroup, :faculty, :language)
            RETURNING id, created_at, updated_at
            """;
        var params = new MapSqlParameterSource()
            .addValue("telegramId", student.getTelegramId())
            .addValue("fullName", student.getFullName())
            .addValue("course", student.getCourse())
            .addValue("groupName", student.getGroupName())
            .addValue("subgroup", student.getSubgroup())
            .addValue("faculty", student.getFaculty())
            .addValue("language", student.getLanguage());
        var keyHolder = new GeneratedKeyHolder();
        jdbc.update(sql, params, keyHolder, new String[]{"id", "created_at", "updated_at"});
        var keys = keyHolder.getKeys();
        if (keys != null) {
            student.setId((Integer) keys.get("id"));
            student.setCreatedAt(JdbcTimeUtils.toLocalDateTime(keys.get("created_at")));
            student.setUpdatedAt(JdbcTimeUtils.toLocalDateTime(keys.get("updated_at")));
        }
        return student;
    }

    public Student getStudentByTelegramId(long telegramId) {
        String sql = """
            SELECT id, telegram_id, full_name, course, group_name, subgroup, faculty, language, created_at, updated_at
            FROM students WHERE telegram_id = :telegramId
            """;
        var results = jdbc.query(sql, Map.of("telegramId", telegramId), STUDENT_MAPPER);
        return results.isEmpty() ? null : results.get(0);
    }

    public Student getStudentById(int id) {
        String sql = """
            SELECT id, telegram_id, full_name, course, group_name, subgroup, faculty, language, created_at, updated_at
            FROM students WHERE id = :id
            """;
        var results = jdbc.query(sql, Map.of("id", id), STUDENT_MAPPER);
        return results.isEmpty() ? null : results.get(0);
    }

    public void updateStudent(Student student) {
        String sql = """
            UPDATE students
            SET course = :course, group_name = :groupName, subgroup = :subgroup, faculty = :faculty, updated_at = :updatedAt
            WHERE id = :id
            """;
        var params = new MapSqlParameterSource()
            .addValue("id", student.getId())
            .addValue("course", student.getCourse())
            .addValue("groupName", student.getGroupName())
            .addValue("subgroup", student.getSubgroup())
            .addValue("faculty", student.getFaculty())
            .addValue("updatedAt", LocalDateTime.now());
        jdbc.update(sql, params);
    }

    public void updateStudentLanguage(int studentId, String language) {
        jdbc.update("UPDATE students SET language = :language, updated_at = :updatedAt WHERE id = :id",
            Map.of("id", studentId, "language", language, "updatedAt", LocalDateTime.now()));
    }

    public void updateStudentName(int studentId, String name) {
        String sql = "UPDATE students SET full_name = :name, updated_at = :updatedAt WHERE id = :id";
        jdbc.update(sql, Map.of("id", studentId, "name", name, "updatedAt", LocalDateTime.now()));
    }

    public boolean studentExists(long telegramId) {
        String sql = "SELECT EXISTS(SELECT 1 FROM students WHERE telegram_id = :telegramId)";
        Boolean exists = jdbc.queryForObject(sql, Map.of("telegramId", telegramId), Boolean.class);
        return Boolean.TRUE.equals(exists);
    }

    public List<Integer> getDistinctCourses() {
        return jdbc.queryForList(
            "SELECT DISTINCT course FROM students WHERE course > 0 ORDER BY course",
            Map.of(),
            Integer.class
        );
    }

    public List<String> getDistinctGroups() {
        return getDistinctTextValues("group_name");
    }

    public List<String> getDistinctSubgroups() {
        return getDistinctTextValues("subgroup");
    }

    public List<String> getDistinctFaculties() {
        return getDistinctTextValues("faculty");
    }

    private List<String> getDistinctTextValues(String columnName) {
        String sql = "SELECT DISTINCT " + columnName + " FROM students " +
            "WHERE " + columnName + " IS NOT NULL AND TRIM(" + columnName + ") <> '' " +
            "ORDER BY " + columnName;
        return jdbc.queryForList(sql, Map.of(), String.class);
    }

    public List<Student> getStudents(StudentFilter filter) {
        var conditions = new ArrayList<String>();
        var params = new MapSqlParameterSource();

        if (filter.getFullName() != null && !filter.getFullName().isEmpty()) {
            String search = filter.getFullName().trim();
            if (search.matches("\\d{1,18}")) {
                long numericSearch = Long.parseLong(search);
                conditions.add("(uz_translit(full_name) LIKE uz_translit(:fullName) OR id = :studentId OR telegram_id = :telegramId)");
                params.addValue("studentId", numericSearch <= Integer.MAX_VALUE ? (int) numericSearch : -1);
                params.addValue("telegramId", numericSearch);
            } else {
                conditions.add("uz_translit(full_name) LIKE uz_translit(:fullName)");
            }
            params.addValue("fullName", "%" + search + "%");
        }
        if (filter.getCourse() > 0) {
            conditions.add("course = :course");
            params.addValue("course", filter.getCourse());
        }
        if (filter.getGroupName() != null && !filter.getGroupName().isEmpty()) {
            conditions.add("LOWER(group_name) LIKE LOWER(:groupName)");
            params.addValue("groupName", "%" + filter.getGroupName() + "%");
        }
        if (filter.getSubgroup() != null && !filter.getSubgroup().isEmpty()) {
            conditions.add("LOWER(subgroup) LIKE LOWER(:subgroup)");
            params.addValue("subgroup", "%" + filter.getSubgroup() + "%");
        }
        if (filter.getFaculty() != null && !filter.getFaculty().isEmpty()) {
            conditions.add("LOWER(faculty) LIKE LOWER(:faculty)");
            params.addValue("faculty", "%" + filter.getFaculty() + "%");
        }

        String where = conditions.isEmpty() ? "" : " WHERE " + String.join(" AND ", conditions);
        int limit = filter.getLimit() > 0 ? filter.getLimit() : 20;
        params.addValue("limit", limit).addValue("offset", filter.getOffset());

        String sql = """
            SELECT id, telegram_id, full_name, course, group_name, subgroup, faculty, language, created_at, updated_at
            FROM students
            """ + where + " ORDER BY full_name LIMIT :limit OFFSET :offset";

        return jdbc.query(sql, params, STUDENT_MAPPER);
    }

    public int countStudents(StudentFilter filter) {
        var conditions = new ArrayList<String>();
        var params = new MapSqlParameterSource();

        if (filter.getFullName() != null && !filter.getFullName().isEmpty()) {
            String search = filter.getFullName().trim();
            if (search.matches("\\d{1,18}")) {
                long numericSearch = Long.parseLong(search);
                conditions.add("(uz_translit(full_name) LIKE uz_translit(:fullName) OR id = :studentId OR telegram_id = :telegramId)");
                params.addValue("studentId", numericSearch <= Integer.MAX_VALUE ? (int) numericSearch : -1);
                params.addValue("telegramId", numericSearch);
            } else {
                conditions.add("uz_translit(full_name) LIKE uz_translit(:fullName)");
            }
            params.addValue("fullName", "%" + search + "%");
        }
        if (filter.getCourse() > 0) {
            conditions.add("course = :course");
            params.addValue("course", filter.getCourse());
        }
        if (filter.getGroupName() != null && !filter.getGroupName().isEmpty()) {
            conditions.add("LOWER(group_name) LIKE LOWER(:groupName)");
            params.addValue("groupName", "%" + filter.getGroupName() + "%");
        }
        if (filter.getSubgroup() != null && !filter.getSubgroup().isEmpty()) {
            conditions.add("LOWER(subgroup) LIKE LOWER(:subgroup)");
            params.addValue("subgroup", "%" + filter.getSubgroup() + "%");
        }
        if (filter.getFaculty() != null && !filter.getFaculty().isEmpty()) {
            conditions.add("LOWER(faculty) LIKE LOWER(:faculty)");
            params.addValue("faculty", "%" + filter.getFaculty() + "%");
        }

        String where = conditions.isEmpty() ? "" : " WHERE " + String.join(" AND ", conditions);
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM students" + where, params, Integer.class);
        return count != null ? count : 0;
    }
}
