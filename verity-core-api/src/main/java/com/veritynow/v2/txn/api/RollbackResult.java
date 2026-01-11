package com.veritynow.v2.txn.api;

public record RollbackResult(
        boolean ok,
        String transactionId,
        String state,
        String message
) {
    public static RollbackResult ok(String txnId) {
        return new RollbackResult(true, txnId, "ROLLED_BACK", "rolled back");
    }
    public static RollbackResult noop(String txnId, String state, String msg) {
        return new RollbackResult(true, txnId, state, msg);
    }
    public static RollbackResult fail(String txnId, String state, String msg) {
        return new RollbackResult(false, txnId, state, msg);
    }
}
