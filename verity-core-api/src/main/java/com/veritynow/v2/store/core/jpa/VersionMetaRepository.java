package com.veritynow.v2.store.core.jpa;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface VersionMetaRepository extends JpaRepository<VersionMetaEntity, Long> {

    // newest-first like FS sorts filenames reversed :contentReference[oaicite:7]{index=7}
    List<VersionMetaEntity> findAllByInode_IdOrderByTimestampDescIdDesc(Long inodeId);
}
