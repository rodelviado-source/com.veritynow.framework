package com.veritynow.core.store.versionstore;

import java.io.IOException;
import java.util.Objects;

import com.veritynow.core.store.lock.LockHandle;
import com.veritynow.core.store.lock.LockingService;

public class CloseableLockHandle implements AutoCloseable {
	private final LockHandle lockHandle;
	private final LockingService lockingService;;
	
	
	public CloseableLockHandle(LockingService lockingService, LockHandle lockHandle) {
		Objects.requireNonNull(lockingService, "lockingService");
		Objects.requireNonNull(lockHandle, "lockHandle");
		this.lockingService = lockingService;
		this.lockHandle = lockHandle;
	}

	public LockHandle handle() {
		return lockHandle;
	}
	
	@Override
	public void close() throws IOException {
		lockingService.release(lockHandle);
	}
	
}
