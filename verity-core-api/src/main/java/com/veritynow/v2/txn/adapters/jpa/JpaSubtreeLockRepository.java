package com.veritynow.v2.txn.adapters.jpa;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface JpaSubtreeLockRepository extends JpaRepository<SubtreeLockEntity, String> {

    @Query("SELECT l FROM SubtreeLockEntity l WHERE l.expiresAtMs > :nowMs")
    List<SubtreeLockEntity> findActive(@Param("nowMs") long nowMs);

    @Modifying
    @Query("DELETE FROM SubtreeLockEntity l WHERE l.lockId = :lockId")
    int deleteByLockId(@Param("lockId") String lockId);
}
