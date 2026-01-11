package com.veritynow.v2.txn.core;

public interface TransactionService {
    TxnHandle begin(String lockRoot, TxnOptions options);
    CommitResult commit(String transactionId);
    RollbackResult rollback(String transactionId, String reason);
}
