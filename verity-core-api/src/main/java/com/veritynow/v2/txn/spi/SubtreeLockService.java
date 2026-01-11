package com.veritynow.v2.txn.spi;

public interface SubtreeLockService {
    LockHandle acquire(String lockRoot, String ownerTxnId, long ttlMs);
    LockHandle renew(String lockId);
    void release(String lockId);
}
