package com.dentistrybot.admin.repository;

import com.dentistrybot.admin.model.AdminUser;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class AdminUserRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public AdminUserRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final RowMapper<AdminUser> MAPPER = (rs, rowNum) -> {
        AdminUser u = new AdminUser();
        u.setId(rs.getInt("id"));
        u.setUsername(rs.getString("username"));
        u.setPasswordHash(rs.getString("password_hash"));
        u.setRole(rs.getString("role"));
        u.setFullName(rs.getString("full_name"));
        u.setCreatedAt(rs.getObject("created_at", java.time.LocalDateTime.class));
        return u;
    };

    public Optional<AdminUser> findByUsername(String username) {
        var results = jdbc.query(
            "SELECT * FROM admin_users WHERE username = :username",
            Map.of("username", username), MAPPER);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    public List<AdminUser> findAll() {
        return jdbc.query("SELECT * FROM admin_users ORDER BY created_at", Map.of(), MAPPER);
    }

    public long count() {
        Long c = jdbc.queryForObject("SELECT COUNT(*) FROM admin_users", Map.of(), Long.class);
        return c != null ? c : 0;
    }

    public AdminUser create(AdminUser user) {
        String sql = """
            INSERT INTO admin_users (username, password_hash, role, full_name)
            VALUES (:username, :passwordHash, :role, :fullName)
            RETURNING id, created_at
            """;
        var params = new MapSqlParameterSource()
            .addValue("username", user.getUsername())
            .addValue("passwordHash", user.getPasswordHash())
            .addValue("role", user.getRole())
            .addValue("fullName", user.getFullName());
        var keyHolder = new GeneratedKeyHolder();
        jdbc.update(sql, params, keyHolder, new String[]{"id", "created_at"});
        var keys = keyHolder.getKeys();
        if (keys != null) {
            user.setId((Integer) keys.get("id"));
        }
        return user;
    }

    public void updateRoleAndName(int id, String role, String fullName) {
        var params = new MapSqlParameterSource()
            .addValue("id", id)
            .addValue("role", role)
            .addValue("fullName", fullName);
        jdbc.update("UPDATE admin_users SET role = :role, full_name = :fullName WHERE id = :id", params);
    }

    public void updatePassword(int id, String passwordHash) {
        jdbc.update(
            "UPDATE admin_users SET password_hash = :hash WHERE id = :id",
            Map.of("id", id, "hash", passwordHash));
    }

    public void delete(int id) {
        jdbc.update("DELETE FROM admin_users WHERE id = :id", Map.of("id", id));
    }

    // ==================== PROFESSOR <-> UNIT ASSIGNMENTS ====================

    public List<Integer> getAssignedUnitIds(int adminUserId) {
        return jdbc.queryForList(
            "SELECT unit_id FROM professor_unit_assignments WHERE admin_user_id = :adminUserId",
            Map.of("adminUserId", adminUserId), Integer.class);
    }

    public boolean isUnitAssigned(int adminUserId, int unitId) {
        Boolean exists = jdbc.queryForObject(
            "SELECT EXISTS(SELECT 1 FROM professor_unit_assignments WHERE admin_user_id = :adminUserId AND unit_id = :unitId)",
            Map.of("adminUserId", adminUserId, "unitId", unitId), Boolean.class);
        return Boolean.TRUE.equals(exists);
    }

    public List<Map<String, Object>> getAssignmentsWithUnitNames(int adminUserId) {
        return jdbc.queryForList("""
            SELECT u.id AS "unitId", u.name AS "name", u.title_uz AS "titleUz"
            FROM professor_unit_assignments pa
            JOIN units u ON pa.unit_id = u.id
            WHERE pa.admin_user_id = :adminUserId
            ORDER BY u.name
            """, Map.of("adminUserId", adminUserId));
    }

    public void assignUnit(int adminUserId, int unitId, Integer assignedBy) {
        jdbc.update("""
            INSERT INTO professor_unit_assignments (admin_user_id, unit_id, assigned_by)
            VALUES (:adminUserId, :unitId, :assignedBy)
            ON CONFLICT (admin_user_id, unit_id) DO NOTHING
            """, new MapSqlParameterSource()
            .addValue("adminUserId", adminUserId)
            .addValue("unitId", unitId)
            .addValue("assignedBy", assignedBy));
    }

    public void unassignUnit(int adminUserId, int unitId) {
        jdbc.update(
            "DELETE FROM professor_unit_assignments WHERE admin_user_id = :adminUserId AND unit_id = :unitId",
            Map.of("adminUserId", adminUserId, "unitId", unitId));
    }
}
