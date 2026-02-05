package com.veritynow.core.store.lock;

import java.util.List;
import java.util.Optional;

public interface LockingService {

    LockHandle acquire(List<String> paths);

    default LockHandle acquireLock(String... paths) {
        return acquire(List.of(paths));
    }

    void release(LockHandle handle);
    
    Optional<List<Long>> findActiveAdvisoryLocks(String txnId); 

    Optional<Long> findActiveAdvisoryLock(String txnId, String path);
    
	LockHandle tryAcquireLock(List<String> paths, int maxAttempts, int delayBetweenAttemptsMs);

	
	
}
