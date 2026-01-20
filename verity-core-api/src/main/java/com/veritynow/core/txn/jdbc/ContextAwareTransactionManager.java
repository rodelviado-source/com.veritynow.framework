package com.veritynow.core.txn.jdbc;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import com.veritynow.core.context.Context;
import com.veritynow.core.context.ContextScope;
import com.veritynow.core.context.ContextSnapshot;
import com.veritynow.core.store.TransactionAware;
import com.veritynow.core.txn.TransactionService;

/**
 * Convenience adapter for developers: provides begin/commit/rollback bound to Context.
 *
 * This is intentionally minimal and does not attempt to solve nested transaction policy.
 */
public class ContextAwareTransactionManager implements TransactionAware<ContextScope> {

    private final TransactionService txnService;

    public ContextAwareTransactionManager(TransactionService txnService) {
        this.txnService = Objects.requireNonNull(txnService);
    }

    @Override
    public Optional<ContextScope> begin() {
        if (!Context.isActive()) {
            ContextSnapshot cs = ContextSnapshot.builder()
                    .correlationId(UUID.randomUUID().toString())
                    .transactionId(UUID.randomUUID().toString())
                    .propagated(false)
                    .build();
            Context.scope(cs);
        }

        ContextScope scope = Context.scope();
        ContextSnapshot snap = Context.snapshot();

        Objects.requireNonNull(snap.correlationId(), "correlationId");
        Objects.requireNonNull(snap.transactionIdOrNull(), "transactionId");

        txnService.begin(snap.transactionIdOrNull());
        return Optional.of(scope);
    }

    @Override
    public void commit() {
        if (!Context.isActive()) return;
        String txnId = Context.snapshot().transactionIdOrNull();
        Objects.requireNonNull(txnId, "transactionId");
        txnService.commit(txnId);
    }

    @Override
    public void rollback() {
        if (!Context.isActive()) return;
        String txnId = Context.snapshot().transactionIdOrNull();
        Objects.requireNonNull(txnId, "transactionId");
        txnService.rollback(txnId);
    }
}
