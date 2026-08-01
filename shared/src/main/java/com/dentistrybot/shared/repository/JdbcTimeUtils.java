package com.dentistrybot.shared.repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;

/**
 * The Postgres JDBC driver returns generated timestamp columns (RETURNING/getGeneratedKeys)
 * as {@link Timestamp}, not {@link LocalDateTime}, so a direct cast throws ClassCastException.
 */
final class JdbcTimeUtils {

    private JdbcTimeUtils() {
    }

    static LocalDateTime toLocalDateTime(Object value) {
        if (value instanceof LocalDateTime localDateTime) return localDateTime;
        if (value instanceof Timestamp timestamp) return timestamp.toLocalDateTime();
        return null;
    }
}
