package com.veritynow.core.store.txn.jooq;

import static com.veritynow.core.store.persistence.jooq.Tables.VN_LOCK_GROUP;

import java.util.Objects;
import java.util.UUID;

import org.jooq.DSLContext;
import org.jooq.Record3;
import org.springframework.transaction.annotation.Transactional;

import com.veritynow.core.store.txn.PublishCoordinator;
import com.veritynow.core.store.txn.TransactionFinalizer;

/**
 * jOOQ publish coordinator:
 * - onCommit(): validates lock ownership + fencing (FOR UPDATE), then delegates to store finalizer
 * - onRollback(): delegates to store finalizer
 */
public class JooqPublishCoordinator implements PublishCoordinator {

    private final DSLContext dsl;
    private final TransactionFinalizer finalizer;

    public JooqPublishCoordinator(DSLContext dsl, TransactionFinalizer finalizer) {
        this.dsl = Objects.requireNonNull(dsl, "dsl");
        this.finalizer = Objects.requireNonNull(finalizer, "finalizer");
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

        finalizer.commit(txnId, lockGroupId, fenceToken);
    }

    @Override
    @Transactional
    public void onRollback(String txnId) {
        Objects.requireNonNull(txnId, "txnId");
        finalizer.rollback(txnId);
    }

    private LockGroup loadLockGroupForUpdate(UUID lockGroupId) {
        Record3<String, Long, Boolean> r = dsl
            .select(VN_LOCK_GROUP.OWNER_ID, VN_LOCK_GROUP.FENCE_TOKEN, VN_LOCK_GROUP.ACTIVE)
            .from(VN_LOCK_GROUP)
            .where(VN_LOCK_GROUP.LOCK_GROUP_ID.eq(lockGroupId))
            .forUpdate()
            .fetchOne();

        if (r == null) {
            throw new IllegalStateException("Missing vn_lock_group row for lockGroupId=" + lockGroupId);
        }

        String ownerId = r.value1();
        Long fenceToken = r.value2();
        Boolean active = r.value3();

        return new LockGroup(ownerId, fenceToken == null ? -1L : fenceToken, active != null && active);
    }

    private record LockGroup(String ownerId, long fenceToken, boolean active) {}
}
