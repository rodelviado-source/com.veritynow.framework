package com.veritynow.v2.txn.impl;

import java.util.Objects;
import java.util.UUID;

import com.veritynow.context.Context;
import com.veritynow.context.ContextScope;
import com.veritynow.context.ContextSnapshot;
import com.veritynow.v2.store.TransactionAware;
import com.veritynow.v2.txn.TransactionService;

/**
 * Convenience adapter for developers: provides begin/commit/rollback bound to Context.
 *
 * This is intentionally minimal and does not attempt to solve nested transaction policy.
 */
public class ContextAwareTransactionManager implements TransactionAware {

    private final TransactionService txnService;

    public ContextAwareTransactionManager(TransactionService txnService) {
        this.txnService = Objects.requireNonNull(txnService);
    }

    @Override
    public ContextScope begin() {
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
        return scope;
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
