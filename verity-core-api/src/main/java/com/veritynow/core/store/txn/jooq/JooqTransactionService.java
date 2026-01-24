package com.veritynow.core.store.txn.jooq;

import static com.veritynow.core.store.persistence.jooq.Tables.VN_TXN_EPOCH;
import static org.jooq.impl.DSL.currentOffsetDateTime;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.jooq.DSLContext;
import org.jooq.Record3;
import org.springframework.transaction.annotation.Transactional;

import com.veritynow.core.lock.LockHandle;
import com.veritynow.core.store.txn.PublishCoordinator;
import com.veritynow.core.store.txn.TransactionService;

/**
 * jOOQ TransactionService backed by vn_txn_epoch.
 *
 * No JPA entities or repositories.
 */
public class JooqTransactionService implements TransactionService {

    private final DSLContext dsl;
    private final PublishCoordinator coordinator;

    public JooqTransactionService(DSLContext dsl, PublishCoordinator coordinator) {
        this.dsl = Objects.requireNonNull(dsl, "dsl");
        this.coordinator = Objects.requireNonNull(coordinator, "coordinator");
    }

    @Override
    @Transactional
    public void begin(String txnId) {
        Objects.requireNonNull(txnId, "txnId");

        dsl.insertInto(VN_TXN_EPOCH)
           .set(VN_TXN_EPOCH.TXN_ID, txnId)
           .set(VN_TXN_EPOCH.STATUS, "IN_FLIGHT")
           .set(VN_TXN_EPOCH.UPDATED_AT, currentOffsetDateTime())
           .onConflict(VN_TXN_EPOCH.TXN_ID)
           .doUpdate()
           .set(VN_TXN_EPOCH.STATUS, "IN_FLIGHT")
           .set(VN_TXN_EPOCH.UPDATED_AT, currentOffsetDateTime())
           .execute();
    }

    @Override
    @Transactional
    public void bindLock(String txnId, LockHandle lock) {
        Objects.requireNonNull(txnId, "txnId");
        Objects.requireNonNull(lock, "lock");

        dsl.update(VN_TXN_EPOCH)
           .set(VN_TXN_EPOCH.LOCK_GROUP_ID, lock.lockGroupId())
           .set(VN_TXN_EPOCH.FENCE_TOKEN, lock.fenceToken())
           .set(VN_TXN_EPOCH.UPDATED_AT, currentOffsetDateTime())
           .where(VN_TXN_EPOCH.TXN_ID.eq(txnId))
           .execute();
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

        coordinator.onCommit(txnId, e.lockGroupId, e.fenceToken);

        dsl.update(VN_TXN_EPOCH)
           .set(VN_TXN_EPOCH.STATUS, "COMMITTED")
           .set(VN_TXN_EPOCH.UPDATED_AT, currentOffsetDateTime())
           .where(VN_TXN_EPOCH.TXN_ID.eq(txnId))
           .execute();
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

        coordinator.onRollback(txnId);

        dsl.update(VN_TXN_EPOCH)
           .set(VN_TXN_EPOCH.STATUS, "ROLLED_BACK")
           .set(VN_TXN_EPOCH.UPDATED_AT, currentOffsetDateTime())
           .where(VN_TXN_EPOCH.TXN_ID.eq(txnId))
           .execute();
    }

    private Optional<Epoch> readEpoch(String txnId) {
        Record3<String, UUID, Long> r = dsl
            .select(VN_TXN_EPOCH.STATUS, VN_TXN_EPOCH.LOCK_GROUP_ID, VN_TXN_EPOCH.FENCE_TOKEN)
            .from(VN_TXN_EPOCH)
            .where(VN_TXN_EPOCH.TXN_ID.eq(txnId))
            .fetchOne();

        if (r == null) return Optional.empty();

        String status = r.value1();
        UUID lockGroupId = r.value2();
        Long fenceToken = r.value3();

        return Optional.of(new Epoch(
            status,
            lockGroupId,
            fenceToken == null ? -1L : fenceToken
        ));
    }

    private record Epoch(String status, UUID lockGroupId, long fenceToken) {}
}
