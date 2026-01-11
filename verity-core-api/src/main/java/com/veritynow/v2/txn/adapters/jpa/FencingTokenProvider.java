package com.veritynow.v2.txn.adapters.jpa;

public interface FencingTokenProvider {
    long nextToken();
}
