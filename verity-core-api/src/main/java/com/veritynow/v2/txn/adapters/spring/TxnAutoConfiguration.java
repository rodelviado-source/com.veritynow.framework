package com.veritynow.v2.txn.adapters.spring;

import com.veritynow.v2.txn.adapters.jpa.*;
import com.veritynow.v2.txn.core.*;
import com.veritynow.v2.txn.spi.*;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.*;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * Turn-key auto-configuration.
 *
 * Defaults are applied only when the application has not provided a bean.
 * JPA-backed adapters are provided when Spring Data JPA repositories are present.
 */
@AutoConfiguration
public class TxnAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public SagaCoordinator sagaCoordinator() {
        return new NullSagaCoordinator();
    }

    @Bean
    @ConditionalOnMissingBean
    public Clock clock() {
        return new SystemClock();
    }

    @Bean
    @ConditionalOnMissingBean
    public TxnIdGenerator txnIdGenerator() {
        return new UuidTxnIdGenerator();
    }

    @Bean
    @ConditionalOnClass(JdbcTemplate.class)
    @ConditionalOnMissingBean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    /**
     * Auto-create sequence once (Postgres).
     * If your environment manages schema via Flyway/Liquibase, you may omit this bean.
     */
    @Bean
    @ConditionalOnProperty(prefix = "veritynow.txn", name = "autoCreateFencingSequence", havingValue = "true", matchIfMissing = true)
    @ConditionalOnBean(JdbcTemplate.class)
    public PostgresSequenceInitializer fencingSequenceInitializer(JdbcTemplate jdbc) {
        return new PostgresSequenceInitializer(jdbc, "vn_fencing_token_seq");
    }

    @Bean
    @ConditionalOnBean(JdbcTemplate.class)
    @ConditionalOnMissingBean
    public FencingTokenProvider fencingTokenProvider(JdbcTemplate jdbc) {
        return new PostgresSequenceFencingTokenProvider(jdbc, "vn_fencing_token_seq");
    }

    @Bean
    @ConditionalOnBean(JpaTxnEntityRepository.class)
    @ConditionalOnMissingBean(TxnRepository.class)
    public TxnRepository txnRepository(JpaTxnEntityRepository repo) {
        return new JpaTxnRepositoryAdapter(repo);
    }

    @Bean
    @ConditionalOnBean({JpaSubtreeLockRepository.class, FencingTokenProvider.class, Clock.class})
    @ConditionalOnMissingBean(SubtreeLockService.class)
    public SubtreeLockService subtreeLockService(JpaSubtreeLockRepository repo, FencingTokenProvider tokenProvider, Clock clock) {
        return new DbSubtreeLockService(repo, tokenProvider, clock);
    }

    @Bean
    @ConditionalOnBean({SubtreeLockService.class, TxnRepository.class, VersionStore.class, EventRecorder.class, SagaCoordinator.class, Clock.class, TxnIdGenerator.class})
    @ConditionalOnMissingBean(TransactionService.class)
    public TransactionService transactionService(
            SubtreeLockService lockService,
            TxnRepository txnRepository,
            VersionStore versionStore,
            EventRecorder eventRecorder,
            SagaCoordinator sagaCoordinator,
            Clock clock,
            TxnIdGenerator txnIdGenerator
    ) {
        return new TransactionServiceImpl(lockService, txnRepository, versionStore, eventRecorder, sagaCoordinator, clock, txnIdGenerator);
    }
}
