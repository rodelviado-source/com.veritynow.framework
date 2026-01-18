package com.veritynow.v2.txn.spi;

import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Store-specific finalization operations.
 *
 * <p>commit:</p>
 * <ul>
 *   <li>Clone all IN_FLIGHT versions for txnId and stamp COMMITTED</li>
 *   <li>Publish/move HEADs to COMMITTED clones, guarded by fenceToken CAS predicate</li>
 * </ul>
 *
 * <p>rollback:</p>
 * <ul>
 *   <li>Clone all IN_FLIGHT versions for txnId and stamp ROLLED_BACK</li>
 *   <li>MUST NOT move HEADs</li>
 * </ul>
 */
public interface StoreTxnFinalizer {

    void commit(String txnId, UUID lockGroupId, long fenceToken, JdbcTemplate jdbc);

    void rollback(String txnId, JdbcTemplate jdbc);
}
