package com.veritynow.v2.txn.impl;

import java.util.Objects;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import com.veritynow.v2.txn.PublishCoordinator;
import com.veritynow.v2.txn.spi.StoreTxnFinalizer;

/**
 * JDBC publish coordinator:
 * - onCommit(): validates lock ownership + fencing, then delegates to store finalizer (clone COMMITTED + fenced HEAD move)
 * - onRollback(): delegates to store finalizer (clone ROLLED_BACK only; no HEAD move)
 */
public class JdbcPublishCoordinator implements PublishCoordinator {

    private final JdbcTemplate jdbc;
    private final StoreTxnFinalizer store;

    public JdbcPublishCoordinator(JdbcTemplate jdbc, StoreTxnFinalizer store) {
        this.jdbc = Objects.requireNonNull(jdbc, "jdbc");
        this.store = Objects.requireNonNull(store, "store");
    }

    @Override
    @Transactional
    public void onCommit(String txnId, UUID lockGroupId, long fenceToken) {
        Objects.requireNonNull(txnId, "txnId");
        Objects.requireNonNull(lockGroupId, "lockGroupId");

        LockGroup lg = loadLockGroupForUpdate(lockGroupId);

        if (!lg.active) {
            throw new IllegalStateException("Lock group not active: " + lockGroupId);
        }
        if (!txnId.equals(lg.ownerId)) {
            throw new IllegalStateException("Lock group owner mismatch. expected=" + txnId + " actual=" + lg.ownerId);
        }
        if (lg.fenceToken != fenceToken) {
            throw new IllegalStateException("Fence token mismatch. expected=" + fenceToken + " actual=" + lg.fenceToken);
        }

        store.commit(txnId, lockGroupId, fenceToken, jdbc);
    }

    @Override
    @Transactional
    public void onRollback(String txnId) {
        Objects.requireNonNull(txnId, "txnId");
        store.rollback(txnId, jdbc);
    }

    private LockGroup loadLockGroupForUpdate(UUID lockGroupId) {
        return jdbc.query(
            "SELECT owner_id, fence_token, active FROM vn_lock_group WHERE lock_group_id = ? FOR UPDATE",
            ps -> ps.setObject(1, lockGroupId),
            rs -> {
                if (!rs.next()) {
                    throw new IllegalStateException("Missing vn_lock_group row for lockGroupId=" + lockGroupId);
                }
                String ownerId = rs.getString("owner_id");
                long fenceToken = rs.getLong("fence_token");
                boolean active = rs.getBoolean("active");
                return new LockGroup(ownerId, fenceToken, active);
            }
        );
    }

    private record LockGroup(String ownerId, long fenceToken, boolean active) {}
}
