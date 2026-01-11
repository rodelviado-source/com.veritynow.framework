package com.veritynow.v2.txn.spi;

public record TxnContext(
        String transactionId,
        String lockRoot,
        String lockId,
        long fencingToken,
        String principal,
        String contextName
) {}
