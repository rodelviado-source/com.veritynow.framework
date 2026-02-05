package com.veritynow.core.store;

import java.util.List;
import java.util.Optional;

import com.veritynow.core.store.versionstore.CloseableLockHandle;



public interface LockingAware<LOCKHANDLE> {
	LOCKHANDLE acquire(List<String> paths);

    default LOCKHANDLE acquireLock(String... paths) {
        return acquire(List.of(paths));
    }

    void release(LOCKHANDLE handle);
    
    Optional<List<Long>> findActiveAdvisoryLocks(String txnId);
    Optional<Long> findActiveAdvisoryLock(String txnId, String path);

	LOCKHANDLE tryAcquireLock(List<String> paths, int maxAttempts, int delayBetweenAttemptsMs);
	LOCKHANDLE tryAcquireLock(String path);
    	
	
}
