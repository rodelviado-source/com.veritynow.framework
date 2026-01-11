package com.veritynow.v2.txn.spi;

import java.util.List;

public interface VersionStore {
    /**
     * List versions created by a transaction, restricted to a lockRoot prefix.
     */
    List<VersionRef> listByTransaction(String transactionId, String lockRootPrefix);

    /**
     * Move HEAD of the path to the specified version (publish step).
     */
    void moveHead(String path, VersionRef version);
}
