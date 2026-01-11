package com.veritynow.v2.txn.core;

public record TxnRecord(
        String txnId,
        String lockRoot,
        String lockId,
        long fencingToken,
        long lockExpiresAtMs,
        State state,
        String principal,
        long createdAtMs,
        long updatedAtMs,
        String failureReason
) {
    public enum State {
        ACTIVE,
        COMMIT_REQUESTED,
        COMMITTED,
        ROLLED_BACK
    }
}
