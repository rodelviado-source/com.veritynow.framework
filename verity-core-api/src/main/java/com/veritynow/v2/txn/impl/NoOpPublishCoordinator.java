package com.veritynow.v2.txn.impl;

import java.util.UUID;

import com.veritynow.v2.txn.PublishCoordinator;

/**
 * Default no-op coordinator for stores that do not implement transactional publish.
 */
public class NoOpPublishCoordinator implements PublishCoordinator {
    @Override
    public void onCommit(String txnId, UUID lockGroupId, long fenceToken) {
        // no-op
    }

    @Override
    public void onRollback(String txnId) {
        // no-op
    }
}
