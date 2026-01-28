package com.veritynow.core.store;

public interface TransactionAndLockingAware<PK, BLOBMETA, VERSIONMETA, CONTEXT> extends VersionStore<PK, BLOBMETA, VERSIONMETA>, LockingAware, TransactionAware<CONTEXT>{

}
