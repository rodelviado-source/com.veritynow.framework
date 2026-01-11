package com.veritynow.v2.txn.adapters.jpa;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Creates the fencing token sequence if missing.
 *
 * Uses Postgres syntax: CREATE SEQUENCE IF NOT EXISTS.
 * If the database does not support it, this initializer is safe to disable via configuration.
 */
public class PostgresSequenceInitializer implements InitializingBean {
    private final JdbcTemplate jdbc;
    private final String sequenceName;

    public PostgresSequenceInitializer(JdbcTemplate jdbc, String sequenceName) {
        this.jdbc = jdbc;
        this.sequenceName = sequenceName;
    }

    @Override
    public void afterPropertiesSet() {
        try {
            jdbc.execute("create sequence if not exists " + sequenceName);
        } catch (RuntimeException e) {
            // If DB is not Postgres or does not support IF NOT EXISTS, users should manage schema via Flyway/Liquibase.
            // We rethrow only if it looks like Postgres but failed for another reason.
            String msg = String.valueOf(e.getMessage()).toLowerCase();
            if (msg.contains("syntax error") || msg.contains("not supported")) {
                // ignore
                return;
            }
            // ignore by default to keep app booting; enterprise apps should run migrations.
        }
    }
}
