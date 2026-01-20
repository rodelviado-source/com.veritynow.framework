package com.veritynow.core.txn.jdbc;

import java.util.Objects;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;

import com.veritynow.core.txn.TransactionFinalizer;

/**
 * StoreTxnFinalizer for the JPA Inode-backed Versioning Store.
 *
 * Tables/columns used (no name drift):
 * - vn_node_version: version_id, inode_id, timestamp, path, operation, principal,
 *   correlation_id, workflow_id, context_name, transaction_id, transaction_result,
 *   hash, name, mime_type, size
 * - vn_node_head: inode_id, version_id, updated_at, fence_token
 */
public class JdbcTransactionFinalizer implements TransactionFinalizer {

    @Override
    public void commit(String txnId, UUID lockGroupId, long fenceToken, JdbcTemplate jdbc) {
        Objects.requireNonNull(txnId, "txnId");
        Objects.requireNonNull(jdbc, "jdbc");

        Counts c = jdbc.query(
            """
            WITH src AS (
              SELECT COUNT(*)::bigint AS expected
              FROM vn_node_version
              WHERE transaction_id = ?
                AND transaction_result = 'IN_FLIGHT'
            ),
            cloned AS (
              INSERT INTO vn_node_version (
                inode_id,
                timestamp,
                path,
                operation,
                principal,
                correlation_id,
                workflow_id,
                context_name,
                transaction_id,
                transaction_result,
                hash,
                name,
                mime_type,
                size
              )
              SELECT
                v.inode_id,
                (extract(epoch from now())*1000)::bigint,
                v.path,
                v.operation,
                v.principal,
                v.correlation_id,
                v.workflow_id,
                v.context_name,
                v.transaction_id,
                'COMMITTED',
                v.hash,
                v.name,
                v.mime_type,
                v.size
              FROM vn_node_version v
              WHERE v.transaction_id = ?
                AND v.transaction_result = 'IN_FLIGHT'
              RETURNING inode_id, version_id
            ),
            published AS (
              INSERT INTO vn_node_head(inode_id, version_id, updated_at, fence_token)
              SELECT c.inode_id, c.version_id, now(), ?
              FROM cloned c
              ON CONFLICT (inode_id) DO UPDATE
              SET version_id = EXCLUDED.version_id,
                  updated_at      = EXCLUDED.updated_at,
                  fence_token     = EXCLUDED.fence_token
              WHERE vn_node_head.fence_token < EXCLUDED.fence_token
              RETURNING inode_id
            )
            SELECT
              (SELECT expected FROM src) AS expected,
              (SELECT COUNT(*)::bigint FROM cloned) AS cloned,
              (SELECT COUNT(*)::bigint FROM published) AS published
            """,
            ps -> {
                ps.setString(1, txnId);
                ps.setString(2, txnId);
                ps.setLong(3, fenceToken);
            },
            rs -> {
                if (!rs.next()) {
                    throw new IllegalStateException("Commit finalization returned no rows");
                }
                long expected = rs.getLong("expected");
                long cloned = rs.getLong("cloned");
                long published = rs.getLong("published");
                return new Counts(expected, cloned, published);
            }
        );

        // Note: jdbc.query(...) above returns a single Counts via the ResultSetExtractor; if it were a list we'd handle differently.
        // Here we already returned Counts in the extractor.

        if (c.expected == 0) {
            // Nothing to finalize; treat as idempotent commit.
            return;
        }
        if (c.cloned != c.expected) {
            throw new IllegalStateException(
                "Commit finalization mismatch: expected=" + c.expected + " cloned=" + c.cloned
            );
        }
        if (c.published != c.expected) {
            throw new IllegalStateException(
                "HEAD publish rejected due to fencing or missing fence_token column: expected=" + c.expected + " published=" + c.published
            );
        }
    }

    @Override
    public void rollback(String txnId, JdbcTemplate jdbc) {
        Objects.requireNonNull(txnId, "txnId");
        Objects.requireNonNull(jdbc, "jdbc");

        // Rollback clones IN_FLIGHT -> ROLLED_BACK, no HEAD movement.
        jdbc.update(
            """
            INSERT INTO vn_node_version (
              inode_id,
              timestamp,
              path,
              operation,
              principal,
              correlation_id,
              workflow_id,
              context_name,
              transaction_id,
              transaction_result,
              hash,
              name,
              mime_type,
              size
            )
            SELECT
              v.inode_id,
              (extract(epoch from now())*1000)::bigint,
              v.path,
              v.operation,
              v.principal,
              v.correlation_id,
              v.workflow_id,
              v.context_name,
              v.transaction_id,
              'ROLLED_BACK',
              v.hash,
              v.name,
              v.mime_type,
              v.size
            FROM vn_node_version v
            WHERE v.transaction_id = ?
              AND v.transaction_result = 'IN_FLIGHT'
            """,
            txnId
        );
    }

    private record Counts(long expected, long cloned, long published) {}
}
