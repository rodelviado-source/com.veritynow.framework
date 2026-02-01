package com.veritynow.core.store;

public interface TransactionAndLockingAware<PK, BLOBMETA, VERSIONMETA, CONTEXT, HANDLE> extends 
              VersionStore<PK, BLOBMETA, VERSIONMETA>, LockingAware<HANDLE>, TransactionAware<CONTEXT>{

}
