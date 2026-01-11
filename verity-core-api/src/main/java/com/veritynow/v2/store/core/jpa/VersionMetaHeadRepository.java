package com.veritynow.v2.store.core.jpa;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface VersionMetaHeadRepository extends JpaRepository<VersionMetaHeadEntity, Long> {

    Optional<VersionMetaHeadEntity> findByInodeId(Long inodeId);

    // Pessimistic lock to serialize HEAD updates for same inode (FS is effectively serialized by filesystem writes)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select h from VersionMetaHeadEntity h where h.inodeId = :inodeId")
    Optional<VersionMetaHeadEntity> findByInodeIdForUpdate(@Param("inodeId") Long inodeId);
}
