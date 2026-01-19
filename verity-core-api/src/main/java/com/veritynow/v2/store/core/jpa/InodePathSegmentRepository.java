package com.veritynow.v2.store.core.jpa;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface InodePathSegmentRepository extends JpaRepository<InodePathSegmentEntity, Long> {

    List<InodePathSegmentEntity> findAllByInode_IdOrderByOrdAsc(Long inodeId);
}
