package com.veritynow.core.context;

/**
 * AutoCloseable scope to temporarily bind a context snapshot.
 * Intended for try-with-resources usage.
 */
public final class ContextScope implements AutoCloseable {
	
    private final ContextStorage storage;
    private boolean closed = false;

    ContextScope(ContextStorage storage) {
        this.storage = storage;
    }

    
    @Override
    public void close() {
        if (closed) return;
        closed = true;
        storage.clear();
    }
}
