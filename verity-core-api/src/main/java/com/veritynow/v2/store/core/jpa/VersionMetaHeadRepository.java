package com.veritynow.v2.store.core.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

public interface VersionMetaHeadRepository extends JpaRepository<VersionMetaHeadEntity, Long> {

    //Optional<VersionMetaHeadEntity> findById(Long id);
    
}
