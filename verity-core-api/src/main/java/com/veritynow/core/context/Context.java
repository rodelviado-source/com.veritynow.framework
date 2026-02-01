package com.veritynow.core.context;

import java.util.UUID;

import util.StringUtils;

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
        return get().getTransactionId().orElse(null);
    }

    /** Returns principal or null if not present. */
    public static String principalOrNull() {
        return get().getPrincipal().orElse(null);
    }
    
    /** Returns principal or null if not present. */
    public static String contextNameOrNull() {
        return get().getContextName().orElse(null);
    }
    
    /** Returns principal or null if not present. */
    public static String workflowIdOrNull() {
        return get().getWorkflowId().orElse(null);
    }

    /** Returns the ensured immutable snapshot of the current context. */
    public static ContextSnapshot snapshot() {
        return get().snapshot();
    }
    
    // This always bind a request-scoped ContextSnapshot.
    // Always pair this with close after use, this prevents servlet 
    //thread-local context leaks across requests, which would otherwise reuse txnId/fenceToken.
    public static ContextScope ensureContext(String contextName) {
        
    	String    	cn = contextName == null ? "(none)" : contextName;
        ContextSnapshot cs;
        if (!isActive()) {
            cs = ContextSnapshot.builder()
                    .workflowId(UUID.randomUUID().toString())
                    .correlationId(UUID.randomUUID().toString())
                    .transactionId(UUID.randomUUID().toString())
                    .contextName(cn)
                    .propagated(false)
                    .build();
        } else {
            boolean wfIdEmpty = StringUtils.isEmpty(Context.workflowIdOrNull());
            boolean txnIdEmpty = StringUtils.isEmpty(Context.transactionIdOrNull());
            boolean ctxNameEmpty = StringUtils.isEmpty(Context.contextNameOrNull());
            boolean propagate = wfIdEmpty || txnIdEmpty || ctxNameEmpty;

            cs = ContextSnapshot.builder()
                    .workflowId(wfIdEmpty ? UUID.randomUUID().toString() : Context.workflowIdOrNull())
                    // correlationId() is guaranteed non-null by the ContextManager (generated if absent)
                    .correlationId(Context.correlationId())
                    .transactionId(txnIdEmpty ? UUID.randomUUID().toString() : Context.transactionIdOrNull())
                    .contextName(ctxNameEmpty ? cn : Context.contextNameOrNull())
                    .propagated(propagate)
                    .build();
        }
        return scope(cs);
    }	
    
}
