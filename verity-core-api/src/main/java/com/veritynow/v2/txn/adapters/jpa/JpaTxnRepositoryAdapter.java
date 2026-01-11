package com.veritynow.v2.txn.adapters.jpa;

import com.veritynow.v2.txn.core.SystemClock;
import com.veritynow.v2.txn.core.TxnRecord;
import com.veritynow.v2.txn.spi.Clock;
import com.veritynow.v2.txn.spi.TxnRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public class JpaTxnRepositoryAdapter implements TxnRepository {
    private final JpaTxnEntityRepository repo;
    private final Clock clock;
    
    public JpaTxnRepositoryAdapter(JpaTxnEntityRepository repo) {
        this(repo, new SystemClock());
    }
    
   

    public JpaTxnRepositoryAdapter(JpaTxnEntityRepository repo, Clock clock) {
        this.repo = repo;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void insert(TxnRecord record) {
        repo.save(TxnEntity.fromRecord(record));
    }

    @Override
    public Optional<TxnRecord> find(String txnId) {
        return repo.findByTxnId(txnId).map(TxnEntity::toRecord);
    }

    @Override
    @Transactional
    public Optional<TxnRecord> transition(String txnId, TxnRecord.State from, TxnRecord.State to, long nowMs, String reason) {
        int updated = repo.transition(txnId, from, to, nowMs, reason);
        if (updated == 0) return Optional.empty();
        return repo.findByTxnId(txnId).map(TxnEntity::toRecord);
    }

    @Override
    @Transactional
    public void touch(String txnId, long nowMs, long lockExpiresAtMs) {
        repo.touch(txnId, nowMs, lockExpiresAtMs);
    }
    

}
