package com.veritynow.core.api.bdo.personal.loans;

import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import com.veritynow.config.RecordEntity;

import java.util.Optional;

public interface RecordRepository extends JpaRepository<RecordEntity, Long> {

	@Query("""
			  select r from RecordEntity r
			  where (:q is null or
			    lower(coalesce(r.status,'')) like lower(concat('%', :q, '%')) or
			    lower(coalesce(r.description,'')) like lower(concat('%', :q, '%')) or
			    lower(coalesce(r.agentFirstName,'')) like lower(concat('%', :q, '%')) or
			    lower(coalesce(r.agentLastName,'')) like lower(concat('%', :q, '%')) or
			    lower(coalesce(r.clientFirstName,'')) like lower(concat('%', :q, '%')) or
			    lower(coalesce(r.clientLastName,'')) like lower(concat('%', :q, '%'))
			  )
			""")
	Page<RecordEntity> search(@Param("q") String q, Pageable pageable);

	Optional<RecordEntity> findByAgentIdAndClientId(String agentId, String clientId);

	void deleteByAgentIdAndClientId(String agentId, String clientId);

	boolean existsByAgentIdAndClientId(String agentId, String clientId);
}
