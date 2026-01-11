package com.veritynow.v2.txn.api;

public record TxnOptions(
        long ttlMs,
        String principal,
        String contextName
) {
    public static TxnOptions defaults() {
        return new TxnOptions(60_000L, "anonymous", "default");
    }
}
