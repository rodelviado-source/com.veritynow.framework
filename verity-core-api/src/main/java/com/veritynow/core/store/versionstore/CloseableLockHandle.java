package com.veritynow.core.store.versionstore;

import java.io.Closeable;
import java.io.IOException;

import com.veritynow.core.store.lock.LockHandle;
import com.veritynow.core.store.lock.LockingService;

public class CloseableLockHandle implements Closeable {
	private final LockHandle lockHandle;
	private final LockingService lockingService;;
	
	
	public CloseableLockHandle(LockingService lockingService, LockHandle lockHandle) {
		this.lockingService = lockingService;
		this.lockHandle = lockHandle;
	}

	public LockHandle handle() {
		return lockHandle;
	}
	
	@Override
	public void close() throws IOException {
		if (lockHandle != null) {
			lockingService.release(lockHandle);
		}
	}
	
}
