package com.veritynow.core.store.txn.jooq;

import static com.veritynow.core.store.persistence.jooq.Tables.VN_LOCK_GROUP;
import static com.veritynow.core.store.persistence.jooq.Tables.VN_TXN_EPOCH;
import static org.jooq.impl.DSL.condition;
import static org.jooq.impl.DSL.currentOffsetDateTime;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.jooq.DSLContext;
import org.jooq.Record3;
import org.springframework.transaction.annotation.Transactional;

import com.veritynow.core.store.lock.LockHandle;
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

        Epoch e0 = readEpoch(txnId).orElseThrow(() ->
            new IllegalStateException("Missing vn_txn_epoch row for txnId=" + txnId)
        );

        // Resolve lock metadata lazily (turnkey commit): if epoch does not yet have
        // (lock_group_id, fence_token), derive it from the active lock group owned
        // by this txnId.
        Epoch e = resolveLockIfMissing(txnId, e0);

        if (!"IN_FLIGHT".equals(e.status)) {
            if ("COMMITTED".equals(e.status)) return; // idempotent
            throw new IllegalStateException("Txn not IN_FLIGHT: " + e.status);
        }

        if (e.lockGroupId == null || e.fenceToken < 0) {
            throw new IllegalStateException(
                "Commit requires an active lock group (lock_group_id + fence_token) owned by txnId=" + txnId
            );
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

        // Best-effort: if we can resolve a held lock group for this txn, stamp it
        // into vn_txn_epoch for auditability. Rollback itself does not require a lock.
        Epoch e = resolveLockIfMissing(txnId, opt.get());
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

    /**
     * Turnkey behavior: derive (lock_group_id, fence_token) for this transaction from
     * the locking kernel, if they have not been stamped into vn_txn_epoch yet.
     */
    private Epoch resolveLockIfMissing(String txnId, Epoch e) {
        if (e.lockGroupId != null && e.fenceToken >= 0) return e;

        Optional<LockHandle> opt = findActiveLockGroupOwnedBy(txnId);
        if (opt.isEmpty()) return e;

        LockHandle h = opt.get();
        dsl.update(VN_TXN_EPOCH)
           .set(VN_TXN_EPOCH.LOCK_GROUP_ID, h.lockGroupId())
           .set(VN_TXN_EPOCH.FENCE_TOKEN, h.fenceToken())
           .set(VN_TXN_EPOCH.UPDATED_AT, currentOffsetDateTime())
           .where(VN_TXN_EPOCH.TXN_ID.eq(txnId))
           .execute();

        return new Epoch(e.status, h.lockGroupId(), h.fenceToken());
    }

    /**
     * Returns the single active lock group owned by {@code ownerId} (txnId), if any.
     *
     * Enforces the invariant that a transaction must hold at most one active lock group.
     */
    private Optional<LockHandle> findActiveLockGroupOwnedBy(String ownerId) {
        var rows = dsl
            .select(VN_LOCK_GROUP.LOCK_GROUP_ID, VN_LOCK_GROUP.FENCE_TOKEN)
            .from(VN_LOCK_GROUP)
            .where(VN_LOCK_GROUP.OWNER_ID.eq(ownerId))
            .and(VN_LOCK_GROUP.ACTIVE.eq(true))
            .and(condition("( {0} is null OR {0} > now() )", VN_LOCK_GROUP.EXPIRES_AT))
            .limit(2)
            .fetch();

        if (rows.isEmpty()) return Optional.empty();
        if (rows.size() > 1) {
            throw new IllegalStateException(
                "Multiple active lock groups owned by ownerId=" + ownerId + ". Expected a single lock group per txn."
            );
        }

        UUID lockGroupId = rows.get(0).value1();
        Long fenceToken = rows.get(0).value2();
        if (lockGroupId == null || fenceToken == null) {
            throw new IllegalStateException(
                "Active lock group missing lock_group_id/fence_token for ownerId=" + ownerId
            );
        }

        // Minimal handle: ownerId is the transaction id.
        return Optional.of(new LockHandle(ownerId, lockGroupId, fenceToken));
    }

    private record Epoch(String status, UUID lockGroupId, long fenceToken) {}
}
