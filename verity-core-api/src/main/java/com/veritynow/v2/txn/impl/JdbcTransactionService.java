package com.veritynow.v2.txn.impl;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import com.veritynow.v2.lock.LockHandle;
import com.veritynow.v2.txn.PublishCoordinator;
import com.veritynow.v2.txn.TransactionService;

/**
 * JDBC TransactionService backed by vn_txn_epoch.
 *
 * No JPA entities or repositories.
 */
public class JdbcTransactionService implements TransactionService {

    private final JdbcTemplate jdbc;
    private final PublishCoordinator coordinator;

    public JdbcTransactionService(JdbcTemplate jdbc, PublishCoordinator coordinator) {
        this.jdbc = Objects.requireNonNull(jdbc, "jdbc");
        this.coordinator = Objects.requireNonNull(coordinator, "coordinator");
    }

    @Override
    @Transactional
    public void begin(String txnId) {
        Objects.requireNonNull(txnId, "txnId");
        jdbc.update(
            "INSERT INTO vn_txn_epoch(txn_id, status) VALUES (?, 'IN_FLIGHT') " +
            "ON CONFLICT (txn_id) DO UPDATE SET status='IN_FLIGHT', updated_at=now()",
            txnId
        );
    }

    @Override
    @Transactional
    public void bindLock(String txnId, LockHandle lock) {
        Objects.requireNonNull(txnId, "txnId");
        Objects.requireNonNull(lock, "lock");
        jdbc.update(
            "UPDATE vn_txn_epoch SET lock_group_id=?, fence_token=?, updated_at=now() WHERE txn_id=?",
            lock.lockGroupId(), lock.fenceToken(), txnId
        );
    }

    @Override
    @Transactional
    public void commit(String txnId) {
        Objects.requireNonNull(txnId, "txnId");

        Epoch e = readEpoch(txnId).orElseThrow(() ->
            new IllegalStateException("Missing vn_txn_epoch row for txnId=" + txnId)
        );

        if (!"IN_FLIGHT".equals(e.status)) {
            if ("COMMITTED".equals(e.status)) return; // idempotent
            throw new IllegalStateException("Txn not IN_FLIGHT: " + e.status);
        }

        if (e.lockGroupId == null || e.fenceToken < 0) {
            throw new IllegalStateException("Commit requires bound lock (lock_group_id + fence_token) for txnId=" + txnId);
        }

        // Commit publishes/moves HEADs and terminalizes versions as COMMITTED
        coordinator.onCommit(txnId, e.lockGroupId, e.fenceToken);

        jdbc.update(
            "UPDATE vn_txn_epoch SET status='COMMITTED', updated_at=now() WHERE txn_id=?",
            txnId
        );
    }

    @Override
    @Transactional
    public void rollback(String txnId) {
        Objects.requireNonNull(txnId, "txnId");

        Optional<Epoch> opt = readEpoch(txnId);
        if (opt.isEmpty()) return; // idempotent rollback

        Epoch e = opt.get();
        if (!"IN_FLIGHT".equals(e.status)) {
            if ("ROLLED_BACK".equals(e.status)) return;
            throw new IllegalStateException("Txn not IN_FLIGHT: " + e.status);
        }

        // Rollback terminalizes versions as ROLLED_BACK and MUST NOT publish/move HEADs
        coordinator.onRollback(txnId);

        jdbc.update(
            "UPDATE vn_txn_epoch SET status='ROLLED_BACK', updated_at=now() WHERE txn_id=?",
            txnId
        );
    }

    private Optional<Epoch> readEpoch(String txnId) {
        return jdbc.query(
            "SELECT status, lock_group_id, fence_token FROM vn_txn_epoch WHERE txn_id=?",
            ps -> ps.setString(1, txnId),
            rs -> {
                if (!rs.next()) return Optional.empty();
                String status = rs.getString("status");
                UUID lockGroupId = (UUID) rs.getObject("lock_group_id");
                long fenceToken = rs.getObject("fence_token") == null ? -1L : rs.getLong("fence_token");
                return Optional.of(new Epoch(status, lockGroupId, fenceToken));
            }
        );
    }

    private record Epoch(String status, UUID lockGroupId, long fenceToken) {}
}
