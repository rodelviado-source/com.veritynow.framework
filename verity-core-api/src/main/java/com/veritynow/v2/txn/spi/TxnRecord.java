package com.veritynow.v2.txn.spi;

public record TxnRecord(
        String transactionId,
        String lockRoot,
        String lockId,
        long fencingToken,
        String principal,
        String contextName,
        String state,
        long createdAtEpochMs,
        long updatedAtEpochMs,
        long lockExpiresAtEpochMs,
        String failureReason
) {}
