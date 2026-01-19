package com.veritynow.core.context;

/**
 * Default ThreadLocal-based context storage.
 */
public final class ThreadLocalContextStorage implements ContextStorage {

    private final ThreadLocal<ContextSnapshot> tl = new ThreadLocal<>();

    @Override
    public ContextSnapshot currentOrNull() {
        return tl.get();
    }

    @Override
    public void bind(ContextSnapshot snapshot) {
        if (snapshot == null) throw new IllegalArgumentException("snapshot must not be null");
        tl.set(snapshot);
    }

    @Override
    public void clear() {
        tl.remove();
    }
}
