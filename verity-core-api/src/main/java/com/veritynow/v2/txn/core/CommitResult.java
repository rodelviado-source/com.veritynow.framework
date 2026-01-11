package com.veritynow.v2.txn.core;

public record CommitResult(boolean ok, TxnRecord.State state, String reason) {
    public static CommitResult isOk() { return new CommitResult(true, TxnRecord.State.COMMITTED, null); }
    public static CommitResult fail(TxnRecord.State state, String reason) { return new CommitResult(false, state, reason); }
}
