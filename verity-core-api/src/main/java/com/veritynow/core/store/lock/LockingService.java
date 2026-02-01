package com.veritynow.core.store.lock;

import java.util.List;
import java.util.Optional;

public interface LockingService {

    LockHandle acquire(List<String> paths);

    default LockHandle acquireLock(String... paths) {
        return acquire(List.of(paths));
    }

    void release(LockHandle handle);
    
    Optional<LockHandle> findActiveLock(String txnId); 

	LockHandle tryAcquireLock(List<String> paths, int maxTries, int intervalBetweenTriesMs, int jitterMs);
}
