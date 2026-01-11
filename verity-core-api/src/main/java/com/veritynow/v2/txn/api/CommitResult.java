package com.veritynow.v2.txn.api;

import java.util.List;

public record CommitResult(
        boolean ok,
        String transactionId,
        String state,
        String message,
        List<String> touchedPaths
) {
    public static CommitResult ok(String txnId, List<String> touched) {
        return new CommitResult(true, txnId, "COMMITTED", "committed", touched);
    }
    public static CommitResult noop(String txnId, String state, String msg) {
        return new CommitResult(true, txnId, state, msg, List.of());
    }
    public static CommitResult fail(String txnId, String state, String msg) {
        return new CommitResult(false, txnId, state, msg, List.of());
    }
}
