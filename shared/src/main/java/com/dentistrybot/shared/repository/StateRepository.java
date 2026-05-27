package com.dentistrybot.shared.repository;

import com.dentistrybot.shared.state.UserState;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Repository
public class StateRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public StateRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final RowMapper<UserState> STATE_MAPPER = (rs, rowNum) -> {
        UserState s = new UserState();
        s.setTelegramId(rs.getLong("telegram_id"));
        s.setBotType(rs.getString("bot_type"));
        s.setState(rs.getString("state"));
        s.setStateData(rs.getString("state_data"));
        s.setUpdatedAt(rs.getObject("updated_at", LocalDateTime.class));
        return s;
    };

    public UserState getUserState(long telegramId, String botType) {
        String sql = """
            SELECT telegram_id, bot_type, state, state_data::text, updated_at
            FROM user_states
            WHERE telegram_id = :telegramId AND bot_type = :botType
            """;
        var params = new MapSqlParameterSource()
            .addValue("telegramId", telegramId)
            .addValue("botType", botType);
        var results = jdbc.query(sql, params, STATE_MAPPER);
        return results.isEmpty() ? null : results.get(0);
    }

    public void setUserState(long telegramId, String botType, String state, String stateDataJson) {
        String sql = """
            INSERT INTO user_states (telegram_id, bot_type, state, state_data, updated_at)
            VALUES (:telegramId, :botType, :state, :stateData::jsonb, :updatedAt)
            ON CONFLICT (telegram_id, bot_type) DO UPDATE
            SET state = :state, state_data = :stateData::jsonb, updated_at = :updatedAt
            """;
        var params = new MapSqlParameterSource()
            .addValue("telegramId", telegramId)
            .addValue("botType", botType)
            .addValue("state", state)
            .addValue("stateData", stateDataJson != null ? stateDataJson : "{}")
            .addValue("updatedAt", LocalDateTime.now());
        jdbc.update(sql, params);
    }

    public void clearUserState(long telegramId, String botType) {
        String sql = """
            UPDATE user_states
            SET state = 'idle', state_data = '{}', updated_at = :updatedAt
            WHERE telegram_id = :telegramId AND bot_type = :botType
            """;
        var params = new MapSqlParameterSource()
            .addValue("telegramId", telegramId)
            .addValue("botType", botType)
            .addValue("updatedAt", LocalDateTime.now());
        jdbc.update(sql, params);
    }

    public void deleteUserState(long telegramId, String botType) {
        String sql = "DELETE FROM user_states WHERE telegram_id = :telegramId AND bot_type = :botType";
        jdbc.update(sql, Map.of("telegramId", telegramId, "botType", botType));
    }

    public List<UserState> getExpiredTestStates() {
        String sql = """
            SELECT telegram_id, bot_type, state, state_data::text, updated_at
            FROM user_states
            WHERE bot_type = 'student' AND state = 'in_test'
              AND (state_data->>'started_at')::timestamptz
                  + ((state_data->>'time_limit_minutes')::int * interval '1 minute')
                  < NOW()
            """;
        return jdbc.query(sql, Map.of(), STATE_MAPPER);
    }

    public List<UserState> getExpiredSituationalStates() {
        String sql = """
            SELECT telegram_id, bot_type, state, state_data::text, updated_at
            FROM user_states
            WHERE bot_type = 'student' AND state IN ('in_situational', 'situational_confirm_answer')
              AND (state_data->>'started_at')::timestamptz
                  + ((state_data->>'time_limit_minutes')::int * interval '1 minute')
                  < NOW()
            """;
        return jdbc.query(sql, Map.of(), STATE_MAPPER);
    }

    public int forceCompleteExpiredTestResults() {
        String sql = """
            UPDATE test_results
            SET status = 'timeout', completed_at = NOW()
            WHERE status = 'in_progress'
              AND id IN (
                SELECT tr.id FROM test_results tr
                JOIN tests t ON tr.test_id = t.id
                WHERE tr.status = 'in_progress'
                  AND tr.started_at + (t.time_limit_minutes * interval '1 minute') < NOW()
              )
            """;
        return jdbc.update(sql, Map.of());
    }
}
