package com.veritynow.v2.txn.spi;
 public enum TransactionState {
        ACTIVE,
        COMMIT_REQUESTED,
        COMMITTED,
        ROLLED_BACK
    }