package com.veritynow.core.store.jpa;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InodeRepository extends JpaRepository<InodeEntity, Long> {

    @Query(value = "select id from vn_inode where scope_key = cast(:scopeKey as ltree)", nativeQuery = true)
    Optional<Long> findIdByScopeKey(@Param("scopeKey") String scopeKey);
}
