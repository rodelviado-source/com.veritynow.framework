package com.veritynow.context;

/**
 * Creates the default ContextManager with safe auto-detection.
 */
public final class ContextBootstrap {

    private ContextBootstrap() {}

    public static ContextManager defaultManager() {
        ContextConfig cfg = ContextConfig.builder().build();
        ContextStorage storage = new ThreadLocalContextStorage();
        IdGenerator idGen = new UuidIdGenerator();

        // Auto-detect SLF4J MDC via reflection (no dependency).
        MdcAdapter mdc = new ReflectionSlf4jMdcAdapter();
        MdcBridge mdcBridge = (mdc != null && mdc.isAvailable()) ? new MdcBridge(mdc, cfg) : null;

        return new DefaultContextManager(storage, idGen, mdcBridge);
    }
}
