package com.veritynow.core.context;

import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;

/**
 * Framework-agnostic execution context facade.
 * Application code should use only this API (and never read headers/MDC directly).
 */
public interface ContextManager {
	
	/** returns true if a context is active false otherwise. */
	boolean isActive();

    /** Always returns a non-null correlation id. Generates one if absent. */
    String getCorrelationId();

    /** Optional business/workflow identifier (never auto-generated). */
    Optional<String> getWorkflowId();

    /** Optional principal (user/service). */
    Optional<String> getPrincipal();
    
    /** Optional contextName (transaction/service). */
    Optional<String> getContextName();
    
    /** Optional transactionId (transaction/service). */
    Optional<String> getTransactionId();

    /** Returns an immutable snapshot of the current context (ensuring correlationId exists). */
    ContextSnapshot snapshot();

    /** Ensures a context exists and returns the ensured snapshot. */
    ContextSnapshot ensure();

    /** Opens a scope for the current (ensured) context. */
    ContextScope scope();

    /** Opens a scope for the given snapshot, binding it as current for the duration of the scope. */
    ContextScope scope(ContextSnapshot snapshot);

    /** Wrap a runnable so it runs with the captured context snapshot. */
    Runnable wrap(Runnable runnable);

    /** Wrap a callable so it runs with the captured context snapshot. */
    <T> Callable<T> wrap(Callable<T> callable);

    /** Wrap an executor so submitted tasks inherit the caller's context snapshot. */
    Executor wrap(Executor executor);
}
