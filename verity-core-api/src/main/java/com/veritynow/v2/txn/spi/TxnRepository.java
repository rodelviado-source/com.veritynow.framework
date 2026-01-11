package com.veritynow.v2.txn.spi;

import com.veritynow.v2.txn.core.TxnRecord;

import java.util.Optional;

public interface TxnRepository {
    void insert(TxnRecord record);
    Optional<TxnRecord> find(String txnId);

    /**
     * Atomic state transition. Returns updated record if transitioned, else empty.
     */
    Optional<TxnRecord> transition(String txnId, TxnRecord.State from, TxnRecord.State to, long nowMs, String reason);

    /**
     * Update lock expiry (server-driven renewal).
     */
    void touch(String txnId, long nowMs, long lockExpiresAtMs);
}
