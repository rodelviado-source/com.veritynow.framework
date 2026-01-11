package com.veritynow.v2.txn.adapters.jpa;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaLockRepository extends JpaRepository<LockEntity, String> {

    @Query("select l from LockEntity l where l.expiresAtEpochMs > :now")
    List<LockEntity> findActive(@Param("now") long now);
}
