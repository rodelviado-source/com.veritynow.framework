package com.veritynow.context;

/**
 * Static access point for the current ContextManager.
 */
public final class Context {

    private static volatile ContextManager INSTANCE = ContextBootstrap.defaultManager();

    private Context() {}

    public static ContextManager get() {
        return INSTANCE;
    }

    /**
     * Replace the global manager (e.g., tests or custom wiring).
     */
    public static void set(ContextManager manager) {
        if (manager == null) throw new IllegalArgumentException("manager must not be null");
        INSTANCE = manager;
    }

    /** Opens a scope ensuring a context exists. */
    public static ContextScope scope() {
        return get().scope();
    }

    /** Opens a scope with an explicit snapshot. */
    public static ContextScope scope(ContextSnapshot snapshot) {
        return get().scope(snapshot);
    }

    // ----------------------------
    // Convenience getters
    // ----------------------------

    public static boolean isActive() {
    	return get().isActive();
    }
    
    /** Always returns a non-null correlation id (generates if absent). */
    public static String correlationId() {
        return get().getCorrelationId();
    }

    /** Returns transaction id or null if not present (never auto-generated). */
    public static String transactionIdOrNull() {
        return get().getWorkflowId().orElse(null);
    }

    /** Returns principal or null if not present. */
    public static String principalOrNull() {
        return get().getPrincipal().orElse(null);
    }
    
    /** Returns principal or null if not present. */
    public static String getContextNameOrNull() {
        return get().getContextName().orElse(null);
    }

    /** Returns the ensured immutable snapshot of the current context. */
    public static ContextSnapshot snapshot() {
        return get().snapshot();
    }
}
