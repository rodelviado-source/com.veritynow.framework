package com.veritynow.v2.txn.api;

public interface TransactionService {
    TxnHandle begin(String lockRoot, TxnOptions options);
    CommitResult commit(String transactionId);
    RollbackResult rollback(String transactionId);
}
