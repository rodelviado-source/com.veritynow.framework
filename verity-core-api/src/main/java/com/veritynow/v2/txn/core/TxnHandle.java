package com.veritynow.v2.txn.core;

public record TxnHandle(
        String transactionId,
        String lockId,
        long fencingToken,
        String lockRoot
) {}
