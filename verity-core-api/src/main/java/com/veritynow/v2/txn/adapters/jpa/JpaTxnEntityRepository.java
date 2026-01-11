package com.veritynow.v2.txn.adapters.jpa;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import com.veritynow.v2.txn.core.TxnRecord;

import java.util.Optional;

public interface JpaTxnEntityRepository extends JpaRepository<TxnEntity, String> {
    Optional<TxnEntity> findByTxnId(String txnId);

    @Modifying
    @Query("UPDATE TxnEntity t SET t.state = :to, t.updatedAtMs = :nowMs, t.failureReason = :reason WHERE t.txnId = :txnId AND t.state = :from")
    int transition(@Param("txnId") String txnId, @Param("from") TxnRecord.State from, @Param("to") TxnRecord.State to, @Param("nowMs") long nowMs, @Param("reason") String reason);

    @Modifying
    @Query("UPDATE TxnEntity t SET t.updatedAtMs = :nowMs, t.lockExpiresAtMs = :lockExpiresAtMs WHERE t.txnId = :txnId")
    int touch(@Param("txnId") String txnId, @Param("nowMs") long nowMs, @Param("lockExpiresAtMs") long lockExpiresAtMs);
}
