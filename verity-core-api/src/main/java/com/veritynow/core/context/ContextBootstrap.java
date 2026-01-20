package com.veritynow.core.context;

/**
 * Creates the default ContextManager with safe auto-detection.
 */
public final class ContextBootstrap {

    private ContextBootstrap() {}

    public static ContextManager defaultManager() {
        ContextStorage storage = new ThreadLocalContextStorage();
        IdGenerator idGen = new UuidIdGenerator();
        return new DefaultContextManager(storage, idGen);
    }
}
