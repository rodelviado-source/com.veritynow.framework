package com.veritynow.v2.txn.core;

public record RollbackResult(boolean ok, TxnRecord.State state, String reason) {
    public static RollbackResult isOk() { return new RollbackResult(true, TxnRecord.State.ROLLED_BACK, null); }
    public static RollbackResult fail(TxnRecord.State state, String reason) { return new RollbackResult(false, state, reason); }
}
