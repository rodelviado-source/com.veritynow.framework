package com.veritynow.context;

/**
 * AutoCloseable scope to temporarily bind a context snapshot.
 * Intended for try-with-resources usage.
 */
public final class ContextScope implements AutoCloseable {

    private final ContextStorage storage;
    private final ContextSnapshot previous;
    private final MdcBridge mdcBridge;
    private boolean closed = false;

    ContextScope(ContextStorage storage, ContextSnapshot previous, MdcBridge mdcBridge) {
        this.storage = storage;
        this.previous = previous;
        this.mdcBridge = mdcBridge;
    }

    @Override
    public void close() {
        if (closed) return;
        closed = true;
        if (previous == null) {
            storage.clear();
            if (mdcBridge != null) mdcBridge.clear();
        } else {
            storage.bind(previous);
            if (mdcBridge != null) mdcBridge.apply(previous);
        }
    }
}
