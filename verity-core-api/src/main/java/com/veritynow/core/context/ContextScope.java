package com.veritynow.core.context;

/**
 * AutoCloseable scope to temporarily bind a context snapshot.
 * Intended for try-with-resources usage.
 */
public final class ContextScope implements AutoCloseable {

    private final ContextStorage storage;
    private final ContextSnapshot previous;
    private boolean closed = false;

    ContextScope(ContextStorage storage, ContextSnapshot previous) {
        this.storage = storage;
        this.previous = previous;
    }

    @Override
    public void close() {
        if (closed) return;
        closed = true;
        if (previous == null) {
            storage.clear();
        } else {
            storage.bind(previous);
        }
    }
}
