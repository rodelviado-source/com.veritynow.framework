package com.veritynow.v2.txn.spi;

public record LockHandle(
        String lockId,
        String lockRoot,
        String ownerTxnId,
        long expiresAtMs,
        long fencingToken
) {}
