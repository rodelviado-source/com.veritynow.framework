package com.veritynow.v2.store.core.jpa;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DirEntryRepository extends JpaRepository<DirEntryEntity, Long> {

    Optional<DirEntryEntity> findByParent_IdAndName(Long parentInodeId, String name);
    
    Optional<DirEntryEntity> findByChild_Id(Long childId);

    List<DirEntryEntity> findAllByParent_Id(Long parentInodeId);
}
