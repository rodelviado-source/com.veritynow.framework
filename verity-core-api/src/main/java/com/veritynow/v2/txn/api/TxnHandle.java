package com.veritynow.v2.txn.api;

public record TxnHandle(
        String transactionId,
        String lockId,
        long fencingToken,
        String lockRoot
) {}
