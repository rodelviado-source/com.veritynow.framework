package com.veritynow.v2.txn.adapters.jpa;

import org.springframework.jdbc.core.JdbcTemplate;

public class PostgresSequenceFencingTokenProvider implements FencingTokenProvider {
    private final JdbcTemplate jdbc;
    private final String sequenceName;

    public PostgresSequenceFencingTokenProvider(JdbcTemplate jdbc, String sequenceName) {
        this.jdbc = jdbc;
        this.sequenceName = sequenceName;
    }

    @Override
    public long nextToken() {
        Long v = jdbc.queryForObject("select nextval('" + sequenceName + "')", Long.class);
        if (v == null) throw new IllegalStateException("Failed to read next fencing token");
        return v;
    }
}
