package com.veritynow.core.store.lock;

import java.util.List;

public interface LockingService {

    LockHandle acquire(List<String> paths);

    default LockHandle acquireLock(String... paths) {
        return acquire(List.of(paths));
    }

    void release(LockHandle handle);
}
