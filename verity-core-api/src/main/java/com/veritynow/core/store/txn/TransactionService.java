package com.veritynow.core.store.txn;

import com.veritynow.core.lock.LockHandle;

/**
 * Minimal transaction lifecycle kernel.
 *
 * <p>Semantics:</p>
 * <ul>
 *   <li><b>begin()</b>: marks txn as IN_FLIGHT</li>
 *   <li><b>bindLock()</b>: binds (lockGroupId,fenceToken) to txn (required for fenced publish)</li>
 *   <li><b>commit()</b>: terminalizes as COMMITTED and publishes/moves HEADs (commit only)</li>
 *   <li><b>rollback()</b>: terminalizes as ROLLED_BACK and does not publish/move HEADs</li>
 * </ul>
 */
public interface TransactionService {
    void begin(String txnId);

    void bindLock(String txnId, LockHandle lock);

    void commit(String txnId);

    void rollback(String txnId);
}
