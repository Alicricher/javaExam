package com.dentistrybot.shared.integration;

import com.zaxxer.hikari.HikariDataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Boots one real Postgres 16 database (matching the image used in docker-compose.yml)
 * for the whole test JVM, runs the actual Flyway migrations against it, and truncates
 * every table before each test. Subclasses get a real NamedParameterJdbcTemplate/
 * TransactionTemplate wired to real repositories/services — no mocks — so SQL that
 * merely *looks* right (e.g. uz_translit(...) matching, retake locking, atomic
 * attempt counting) is actually verified to run correctly against Postgres.
 *
 * By default this manages its own Testcontainers Postgres instance (the normal path
 * for a developer machine or CI with Docker available to the JVM directly). If
 * IT_PG_HOST is set, it connects to that already-running Postgres instead — useful
 * where the JVM can't launch containers itself but one is reachable on the network
 * (e.g. a "services:" container in GitHub Actions, or a Postgres started by a
 * sibling `docker run`).
 */
public abstract class AbstractIntegrationTest {

    protected static final NamedParameterJdbcTemplate jdbc;
    protected static final TransactionTemplate tx;

    static {
        HikariDataSource dataSource = new HikariDataSource();
        String externalHost = System.getenv("IT_PG_HOST");
        if (externalHost != null) {
            String port = System.getenv().getOrDefault("IT_PG_PORT", "5432");
            String db = System.getenv().getOrDefault("IT_PG_DB", "test");
            dataSource.setJdbcUrl("jdbc:postgresql://" + externalHost + ":" + port + "/" + db);
            dataSource.setUsername(System.getenv().getOrDefault("IT_PG_USER", "test"));
            dataSource.setPassword(System.getenv().getOrDefault("IT_PG_PASSWORD", "test"));
        } else {
            PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");
            postgres.start();
            dataSource.setJdbcUrl(postgres.getJdbcUrl());
            dataSource.setUsername(postgres.getUsername());
            dataSource.setPassword(postgres.getPassword());
        }

        Flyway.configure()
            .dataSource(dataSource)
            .locations("classpath:db/migration")
            .load()
            .migrate();

        jdbc = new NamedParameterJdbcTemplate(dataSource);
        tx = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
    }

    @BeforeEach
    void cleanDatabase() {
        jdbc.getJdbcOperations().execute("""
            TRUNCATE TABLE
                test_answers, test_results, test_retakes,
                situational_answers, situational_retakes,
                answer_options, questions, tests,
                theory_materials, situational_tasks, lessons, units,
                admin_users, admins, students, user_states
            RESTART IDENTITY CASCADE
            """);
    }
}
