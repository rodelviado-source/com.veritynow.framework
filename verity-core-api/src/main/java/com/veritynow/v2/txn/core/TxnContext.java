package com.veritynow.v2.txn.core;

public record TxnContext(
        String txnId,
        String lockRoot,
        String lockId,
        long fencingToken,
        String principal
) {}
