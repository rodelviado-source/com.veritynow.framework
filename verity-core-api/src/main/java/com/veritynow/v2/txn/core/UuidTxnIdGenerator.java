package com.veritynow.v2.txn.core;

import com.veritynow.v2.txn.spi.TxnIdGenerator;

import java.util.UUID;

public class UuidTxnIdGenerator implements TxnIdGenerator {
    @Override
    public String newTxnId() {
        return UUID.randomUUID().toString();
    }
}
