package com.veritynow.core.store.txn;

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
public interface TransactionFinalizer {
	
	void begin(String txnId);	

    void commit(String txnId);

    void rollback(String txnId);
}
