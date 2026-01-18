package com.veritynow.v2.store.core.jpa;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface VersionMetaHeadRepository extends JpaRepository<VersionMetaHeadEntity, Long> {

    Optional<VersionMetaHeadEntity> findById(Long id);

//    @Query("select h from VersionMetaHeadEntity h where h.inodeId = :inodeId")
//    Optional<VersionMetaHeadEntity> findByInodeIdForUpdate(@Param("inodeId") Long inodeId);
    
    
    
    
}
