package com.veritynow.v2.txn.core;

public record TxnOptions(long ttlMs, String principal) {
    public static TxnOptions defaults() { return new TxnOptions(60_000, null); }
}
