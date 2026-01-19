package com.veritynow.core.store.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

public interface VersionMetaHeadRepository extends JpaRepository<VersionMetaHeadEntity, Long> {

    //Optional<VersionMetaHeadEntity> findById(Long id);
    
}
