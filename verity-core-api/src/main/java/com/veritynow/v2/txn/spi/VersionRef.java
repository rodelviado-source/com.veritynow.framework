package com.veritynow.v2.txn.spi;

public record VersionRef(String path, String hash, long timestampMs, String operation) {}
