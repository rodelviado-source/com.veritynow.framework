package com.veritynow.v2.txn.adapters.memory;


import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.veritynow.v2.txn.spi.TransactionState;
import com.veritynow.v2.txn.spi.TxnRecord;
import com.veritynow.v2.txn.spi.TxnRepository;

/**
 * Simple in-memory TxnRepository for zero-infra development and unit tests.
 *
 * This is not intended for multi-node deployments.
 */
public class InMemoryTxnRepository implements TxnRepository {

    
    private final ConcurrentMap<String, TxnRecord> map = new ConcurrentHashMap<>();

    @Override
    public void insert(TxnRecord record) {
        TxnRecord prev = map.putIfAbsent(record.transactionId(), record);
        if (prev != null) {
            throw new IllegalStateException("Txn already exists: " + record.transactionId());
        }
    }

    @Override
    public Optional<TxnRecord> find(String txnId) {
        return Optional.ofNullable(map.get(txnId));
    }

    @Override
    public Optional<TxnRecord> transition(String txnId, TransactionState from, TransactionState to, long nowMs, String reason) {
        return Optional.ofNullable(map.compute(txnId, (id, existing) -> {
            if (existing == null) return null;
            if (TransactionState.valueOf(existing.state()) != from) return existing;
            
//            String transactionId,
//            String lockRoot,
//            String lockId,
//            long fencingToken,
//            String principal,
//            String contextName,
//            String state,
//            long createdAtEpochMs,
//            long updatedAtEpochMs,
//            long lockExpiresAtEpochMs,
//            String failureReason
            
            return new TxnRecord(
                    existing.transactionId(),
                    existing.lockRoot(),
                    existing.lockId(),
                    existing.fencingToken(),
                    existing.principal(),
                    existing.contextName(),
                    to.name(),
                    existing.createdAtEpochMs(),
                    nowMs,
                    existing.lockExpiresAtEpochMs(),
                    reason
            );
        })).filter(r -> TransactionState.valueOf(r.state()) == to);
    }

    @Override
    public void touch(String txnId, long nowMs, long lockExpiresAtMs) {
        map.computeIfPresent(txnId, (id, existing) -> 
        new TxnRecord(
                existing.transactionId(),
                existing.lockRoot(),
                existing.lockId(),
                existing.fencingToken(),
                existing.principal(),
                existing.contextName(),
                existing.state(),
                existing.createdAtEpochMs(),
                nowMs,
                lockExpiresAtMs,
                existing.failureReason()
        ));
    }
}
