package com.veritynow.v2.txn.spi;

import com.veritynow.v2.txn.core.TxnContext;

import java.util.List;

public interface EventRecorder {
    void recordTxnBegan(TxnContext ctx);
    void recordCommitRequested(TxnContext ctx);
    void recordTxnCommitted(TxnContext ctx, List<String> touchedPaths);
    void recordTxnRolledBack(TxnContext ctx, String reason);
}
