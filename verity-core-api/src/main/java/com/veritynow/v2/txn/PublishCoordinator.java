package com.veritynow.v2.txn;

import java.util.UUID;

/**
 * Store integration seam.
 *
 * <p>Rules:</p>
 * <ul>
 *   <li>Commit publishes/moves HEADs and terminalizes txn versions as COMMITTED</li>
 *   <li>Rollback terminalizes txn versions as ROLLED_BACK and MUST NOT publish/move HEADs</li>
 * </ul>
 */
public interface PublishCoordinator {

    void onCommit(String txnId, UUID lockGroupId, long fenceToken);

    void onRollback(String txnId);
}
