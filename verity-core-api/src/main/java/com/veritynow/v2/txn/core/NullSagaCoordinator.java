package com.veritynow.v2.txn.core;

import com.veritynow.v2.txn.spi.SagaCoordinator;
import com.veritynow.v2.txn.spi.SagaOutcome;
import com.veritynow.v2.txn.spi.SagaPlan;

public class NullSagaCoordinator implements SagaCoordinator {
    @Override
    public SagaPlan init(TxnContext ctx) {
        return SagaPlan.none();
    }

    @Override
    public SagaOutcome prepareCommit(TxnContext ctx, SagaPlan plan) {
        return SagaOutcome.isOk();
    }

    @Override
    public void rollback(TxnContext ctx, SagaPlan plan) {
        // no-op
    }
}
