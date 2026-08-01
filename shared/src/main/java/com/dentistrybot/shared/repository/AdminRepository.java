package com.dentistrybot.shared.repository;

import com.dentistrybot.shared.model.Admin;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Map;

@Repository
public class AdminRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public AdminRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final RowMapper<Admin> ADMIN_MAPPER = (rs, rowNum) -> {
        Admin a = new Admin();
        a.setId(rs.getInt("id"));
        a.setTelegramId(rs.getLong("telegram_id"));
        a.setPasswordHash(rs.getString("password_hash"));
        a.setAuthenticated(rs.getBoolean("is_authenticated"));
        a.setSessionExpiresAt(rs.getObject("session_expires_at", LocalDateTime.class));
        a.setCreatedAt(rs.getObject("created_at", LocalDateTime.class));
        return a;
    };

    public Admin createAdmin(Admin admin) {
        String sql = """
            INSERT INTO admins (telegram_id, password_hash)
            VALUES (:telegramId, :passwordHash)
            RETURNING id, created_at
            """;
        var params = new MapSqlParameterSource()
            .addValue("telegramId", admin.getTelegramId())
            .addValue("passwordHash", admin.getPasswordHash());
        var keyHolder = new GeneratedKeyHolder();
        jdbc.update(sql, params, keyHolder, new String[]{"id", "created_at"});
        var keys = keyHolder.getKeys();
        if (keys != null) {
            admin.setId((Integer) keys.get("id"));
            admin.setCreatedAt(JdbcTimeUtils.toLocalDateTime(keys.get("created_at")));
        }
        return admin;
    }

    public Admin getAdminByTelegramId(long telegramId) {
        String sql = """
            SELECT id, telegram_id, password_hash, is_authenticated, session_expires_at, created_at
            FROM admins WHERE telegram_id = :telegramId
            """;
        var results = jdbc.query(sql, Map.of("telegramId", telegramId), ADMIN_MAPPER);
        return results.isEmpty() ? null : results.get(0);
    }

    public void updateAdminSession(long telegramId, boolean authenticated, LocalDateTime expiresAt) {
        String sql = """
            UPDATE admins
            SET is_authenticated = :authenticated, session_expires_at = :expiresAt
            WHERE telegram_id = :telegramId
            """;
        jdbc.update(sql, Map.of(
            "telegramId", telegramId,
            "authenticated", authenticated,
            "expiresAt", expiresAt
        ));
    }

    public boolean isAdminAuthenticated(long telegramId) {
        String sql = """
            SELECT is_authenticated AND (session_expires_at IS NULL OR session_expires_at > NOW())
            FROM admins WHERE telegram_id = :telegramId
            """;
        Boolean result = jdbc.queryForObject(sql, Map.of("telegramId", telegramId), Boolean.class);
        return Boolean.TRUE.equals(result);
    }

    public boolean adminExists(long telegramId) {
        String sql = "SELECT EXISTS(SELECT 1 FROM admins WHERE telegram_id = :telegramId)";
        Boolean exists = jdbc.queryForObject(sql, Map.of("telegramId", telegramId), Boolean.class);
        return Boolean.TRUE.equals(exists);
    }

    public void logoutAdmin(long telegramId) {
        String sql = """
            UPDATE admins
            SET is_authenticated = FALSE, session_expires_at = NULL
            WHERE telegram_id = :telegramId
            """;
        jdbc.update(sql, Map.of("telegramId", telegramId));
    }
}
