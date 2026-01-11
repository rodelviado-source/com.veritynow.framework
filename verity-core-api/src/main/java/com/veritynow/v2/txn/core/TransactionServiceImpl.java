package com.veritynow.v2.txn.core;

import com.veritynow.v2.txn.spi.*;

import java.util.*;

public class TransactionServiceImpl implements TransactionService {
    private final SubtreeLockService lockService;
    private final TxnRepository txnRepository;
    private final VersionStore versionStore;
    private final EventRecorder eventRecorder;
    private final SagaCoordinator sagaCoordinator;
    private final Clock clock;
    private final TxnIdGenerator txnIdGenerator;

    public TransactionServiceImpl(
            SubtreeLockService lockService,
            TxnRepository txnRepository,
            VersionStore versionStore,
            EventRecorder eventRecorder,
            SagaCoordinator sagaCoordinator,
            Clock clock,
            TxnIdGenerator txnIdGenerator
    ) {
        this.lockService = Objects.requireNonNull(lockService);
        this.txnRepository = Objects.requireNonNull(txnRepository);
        this.versionStore = Objects.requireNonNull(versionStore);
        this.eventRecorder = Objects.requireNonNull(eventRecorder);
        this.sagaCoordinator = Objects.requireNonNull(sagaCoordinator);
        this.clock = Objects.requireNonNull(clock);
        this.txnIdGenerator = Objects.requireNonNull(txnIdGenerator);
    }

    @Override
    public TxnHandle begin(String lockRoot, TxnOptions options) {
        Objects.requireNonNull(lockRoot);
        if (options == null) options = TxnOptions.defaults();

        String txnId = txnIdGenerator.newTxnId();
        long now = clock.nowMs();

        LockHandle lock = lockService.acquire(lockRoot, txnId, options.ttlMs());

        TxnRecord record = new TxnRecord(
                txnId,
                lock.lockRoot(),
                lock.lockId(),
                lock.fencingToken(),
                lock.expiresAtMs(),
                TxnRecord.State.ACTIVE,
                options.principal(),
                now,
                now,
                null
        );
        txnRepository.insert(record);

        eventRecorder.recordTxnBegan(new TxnContext(txnId, lock.lockRoot(), lock.lockId(), lock.fencingToken(), options.principal()));

        return new TxnHandle(txnId, lock.lockId(), lock.fencingToken(), lock.lockRoot());
    }

    @Override
    public CommitResult commit(String transactionId) {
        Objects.requireNonNull(transactionId);
        TxnRecord existing = txnRepository.find(transactionId).orElse(null);
        if (existing == null) {
            return CommitResult.fail(null, "Transaction not found");
        }
        if (existing.state() == TxnRecord.State.COMMITTED) {
            return CommitResult.isOk();
        }
        if (existing.state() == TxnRecord.State.ROLLED_BACK) {
            return CommitResult.fail(TxnRecord.State.ROLLED_BACK, "Transaction already rolled back");
        }

        long now = clock.nowMs();
        Optional<TxnRecord> transitioned = txnRepository.transition(transactionId, TxnRecord.State.ACTIVE, TxnRecord.State.COMMIT_REQUESTED, now, null);
        if (transitioned.isEmpty()) {
            // someone else changed state; re-check
            TxnRecord reread = txnRepository.find(transactionId).orElse(existing);
            if (reread.state() == TxnRecord.State.COMMITTED) return CommitResult.isOk();
            return CommitResult.fail(reread.state(), "Unable to transition transaction to COMMIT_REQUESTED");
        }
        TxnRecord txn = transitioned.get();

        // Defensive: renew lock (server-driven)
        try {
            LockHandle renewed = lockService.renew(txn.lockId());
            txnRepository.touch(transactionId, clock.nowMs(), renewed.expiresAtMs());
        } catch (RuntimeException e) {
            // lock missing/expired: rollback
            rollback(transactionId, "Lock invalid/expired during commit: " + e.getMessage());
            return CommitResult.fail(TxnRecord.State.ROLLED_BACK, "Lock invalid/expired");
        }

        TxnContext ctx = new TxnContext(txn.txnId(), txn.lockRoot(), txn.lockId(), txn.fencingToken(), txn.principal());
        eventRecorder.recordCommitRequested(ctx);

        // Saga gate (defaults to no-op)
        SagaPlan plan = sagaCoordinator.init(ctx);
        SagaOutcome outcome = sagaCoordinator.prepareCommit(ctx, plan);
        if (!outcome.ok()) {
            try { sagaCoordinator.rollback(ctx, plan); } catch (RuntimeException ignored) {}
            rollback(transactionId, outcome.reason() != null ? outcome.reason() : "Saga blocked commit");
            return CommitResult.fail(TxnRecord.State.ROLLED_BACK, outcome.reason());
        }

        // Compute winners per path (last-write-wins by timestamp)
        List<VersionRef> refs = versionStore.listByTransaction(transactionId, ensurePrefix(txn.lockRoot()));
        Map<String, VersionRef> winners = new HashMap<>();
        for (VersionRef ref : refs) {
            VersionRef prev = winners.get(ref.path());
            if (prev == null || ref.timestampMs() >= prev.timestampMs()) {
                winners.put(ref.path(), ref);
            }
        }

        List<String> touchedPaths = new ArrayList<>(winners.keySet());
        touchedPaths.sort(String::compareTo);

        for (String path : touchedPaths) {
            versionStore.moveHead(path, winners.get(path));
        }

        // finalize state
        Optional<TxnRecord> committed = txnRepository.transition(transactionId, TxnRecord.State.COMMIT_REQUESTED, TxnRecord.State.COMMITTED, clock.nowMs(), null);
        if (committed.isEmpty()) {
            // On crash/retry, HEAD moves are idempotent; state can be fixed by recovery.
            return CommitResult.fail(TxnRecord.State.COMMIT_REQUESTED, "Publish done, but failed to mark COMMITTED");
        }

        eventRecorder.recordTxnCommitted(ctx, touchedPaths);

        try { lockService.release(txn.lockId()); } catch (RuntimeException ignored) {}

        return CommitResult.isOk();
    }

    @Override
    public RollbackResult rollback(String transactionId, String reason) {
        Objects.requireNonNull(transactionId);
        TxnRecord existing = txnRepository.find(transactionId).orElse(null);
        if (existing == null) {
            return RollbackResult.fail(null, "Transaction not found");
        }
        if (existing.state() == TxnRecord.State.ROLLED_BACK) {
            return RollbackResult.isOk();
        }
        if (existing.state() == TxnRecord.State.COMMITTED) {
            return RollbackResult.fail(TxnRecord.State.COMMITTED, "Transaction already committed");
        }

        long now = clock.nowMs();
        TxnRecord.State from = existing.state();
        if (from != TxnRecord.State.ACTIVE && from != TxnRecord.State.COMMIT_REQUESTED) {
            return RollbackResult.fail(from, "Invalid transaction state for rollback");
        }

        Optional<TxnRecord> rolled = txnRepository.transition(transactionId, from, TxnRecord.State.ROLLED_BACK, now, reason);
        if (rolled.isEmpty()) {
            TxnRecord reread = txnRepository.find(transactionId).orElse(existing);
            if (reread.state() == TxnRecord.State.ROLLED_BACK) return RollbackResult.isOk();
            return RollbackResult.fail(reread.state(), "Unable to mark transaction rolled back");
        }
        TxnRecord txn = rolled.get();

        TxnContext ctx = new TxnContext(txn.txnId(), txn.lockRoot(), txn.lockId(), txn.fencingToken(), txn.principal());
        try { sagaCoordinator.rollback(ctx, sagaCoordinator.init(ctx)); } catch (RuntimeException ignored) {}
        eventRecorder.recordTxnRolledBack(ctx, reason);

        try { lockService.release(txn.lockId()); } catch (RuntimeException ignored) {}
        return RollbackResult.isOk();
    }

    private static String ensurePrefix(String lockRoot) {
        String norm = lockRoot.trim();
        if (norm.endsWith("/")) norm = norm.substring(0, norm.length()-1);
        return norm + "/";
    }
}
