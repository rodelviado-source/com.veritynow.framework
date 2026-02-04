package com.veritynow.core.store.txn.jooq;

import java.util.Objects;

import com.veritynow.core.context.Context;
import com.veritynow.core.context.ContextScope;
import com.veritynow.core.store.TransactionAware;
import com.veritynow.core.store.txn.TransactionService;

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
    public String begin() {
		if (!Context.isActive()) {
        	Context.ensureContext("Transaction(begin) Manager Context");
        }    

        Objects.requireNonNull(Context.correlationId(), "correlationId");
        Objects.requireNonNull(Context.transactionIdOrNull(), "transactionId");

        return txnService.begin(Context.transactionIdOrNull());
    }

    @Override
    public void commit() {
        if (!Context.isActive()) return;
        String txnId = Context.transactionIdOrNull();
        Objects.requireNonNull(txnId, "transactionId");
        txnService.commit(txnId);
    }

    @Override
    public void rollback() {
        if (!Context.isActive()) return;
        String txnId = Context.transactionIdOrNull();
        Objects.requireNonNull(txnId, "transactionId");
        txnService.rollback(txnId);
    }
}
