package com.veritynow.core.store;

import java.util.List;
import java.util.Optional;

import com.veritynow.core.store.lock.LockHandle;

public interface LockingAware {
	
	Optional<LockHandle> acquire(List<String> paths);

    default Optional<LockHandle> acquireLock(String... paths) {
        return acquire(List.of(paths));
    }

    void release(LockHandle handle);
}
