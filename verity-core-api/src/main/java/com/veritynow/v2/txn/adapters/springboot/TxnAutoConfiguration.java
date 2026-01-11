package com.veritynow.v2.txn.adapters.springboot;

import jakarta.persistence.EntityManager;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import com.veritynow.v2.txn.api.TransactionService;
import com.veritynow.v2.txn.core.NullSagaCoordinator;
import com.veritynow.v2.txn.core.SystemClock;
import com.veritynow.v2.txn.core.TransactionServiceImpl;
import com.veritynow.v2.txn.core.UuidTxnIdGenerator;
import com.veritynow.v2.txn.adapters.jpa.DbFencingTokenSequence;
import com.veritynow.v2.txn.adapters.jpa.DbSubtreeLockService;
import com.veritynow.v2.txn.adapters.jpa.FencingTokenProvider;
import com.veritynow.v2.txn.adapters.jpa.JpaLockRepository;
import com.veritynow.v2.txn.adapters.jpa.JpaTxnRepository;
import com.veritynow.v2.txn.adapters.jpa.JpaTxnRepositoryAdapter;
import com.veritynow.v2.txn.spi.Clock;
import com.veritynow.v2.txn.spi.EventRecorder;
import com.veritynow.v2.txn.spi.SagaCoordinator;
import com.veritynow.v2.txn.spi.SubtreeLockService;
import com.veritynow.v2.txn.spi.TxnIdGenerator;
import com.veritynow.v2.txn.spi.TxnRepository;
import com.veritynow.v2.txn.spi.VersionStore;

@AutoConfiguration
@ConditionalOnClass({AutoConfiguration.class})
@EntityScan(basePackages = "com.veritynow.v2.txn.adapters.jpa")
@EnableJpaRepositories(basePackages = "com.veritynow.v2.txn.adapters.jpa")
public class TxnAutoConfiguration {

    // ---- Core defaults ----

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

    // ---- JPA-backed TxnRepository ----

    @Bean
    @ConditionalOnBean(JpaTxnRepository.class)
    @ConditionalOnMissingBean(TxnRepository.class)
    public TxnRepository txnRepository(JpaTxnRepository repo, Clock clock) {
        return new JpaTxnRepositoryAdapter(repo, clock);
    }

    // ---- DB fencing token sequence ----

    @Bean
    @ConditionalOnClass(EntityManager.class)
    @ConditionalOnBean(EntityManager.class)
    @ConditionalOnMissingBean(FencingTokenProvider.class)
    public FencingTokenProvider fencingTokenSequence(EntityManager em) {
        return new DbFencingTokenSequence(em);
    }

    // ---- JPA-backed lock service ----

    @Bean
    @ConditionalOnBean(JpaLockRepository.class)
    @ConditionalOnMissingBean(SubtreeLockService.class)
    public SubtreeLockService subtreeLockService(
            JpaLockRepository repo,
            Clock clock,
            DbSubtreeLockService.FencingTokenSequence seq
    ) {
        return new DbSubtreeLockService(repo, clock, seq);
    }

    // ---- TransactionService (only when mandatory deps exist) ----

    @Bean
    @ConditionalOnBean({SubtreeLockService.class, TxnRepository.class, VersionStore.class, EventRecorder.class})
    @ConditionalOnMissingBean(TransactionService.class)
    public TransactionService transactionService(
            SubtreeLockService lockService,
            TxnRepository txnRepo,
            VersionStore versionStore,
            EventRecorder events,
            SagaCoordinator saga,
            TxnIdGenerator ids,
            Clock clock
    ) {
        return new TransactionServiceImpl(lockService, txnRepo, versionStore, events, saga, ids, clock);
    }
}
