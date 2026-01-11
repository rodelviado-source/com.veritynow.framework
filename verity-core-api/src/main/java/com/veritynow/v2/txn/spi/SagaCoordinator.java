package com.veritynow.v2.txn.spi;

import com.veritynow.v2.txn.core.TxnContext;

public interface SagaCoordinator {
    SagaPlan init(TxnContext ctx);

    /**
     * Prepare external steps for commit. Return ok=false to block local publish.
     */
    SagaOutcome prepareCommit(TxnContext ctx, SagaPlan plan);

    /**
     * Best-effort rollback/compensate external steps.
     */
    void rollback(TxnContext ctx, SagaPlan plan);
}
