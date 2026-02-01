package com.veritynow.core.store;

import java.util.List;
import java.util.Optional;



public interface LockingAware<LOCKHANDLE> {
	LOCKHANDLE acquire(List<String> paths);

    default LOCKHANDLE acquireLock(String... paths) {
        return acquire(List.of(paths));
    }

    void release(LOCKHANDLE handle);
    
    Optional<LOCKHANDLE> findActiveLock(String txnId); 

	LOCKHANDLE tryAcquireLock(List<String> paths, int maxTries, int intervalBetweenTriesMs, int jitterMs);


}
