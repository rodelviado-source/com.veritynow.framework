package com.veritynow.core.context;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;

/**
 * Default ContextManager implementation.
 */
final class DefaultContextManager implements ContextManager {

    private final ContextStorage storage; 
    
    DefaultContextManager(ContextStorage storage) {
        this.storage = storage;
    }
    
    @Override
	public boolean isActive() {
    	return storage.currentOrNull() != null;
	}

	@Override
    public String getCorrelationId() {
        return ensure().correlationId();
    }

    @Override
    public Optional<String> getWorkflowId() {
        return Optional.ofNullable(ensure().workflowIdOrNull());
    }

    @Override
    public Optional<String> getPrincipal() {
        return Optional.ofNullable(ensure().principalOrNull());
    }
  
    @Override
	public Optional<String> getContextName() {
    	return Optional.ofNullable(ensure().contextNameOrNull());
	}
    
    @Override
	public Optional<String> getTransactionId() {
    	return Optional.ofNullable(ensure().transactionIdOrNull());
	}

	@Override
    public ContextSnapshot snapshot() {
        return ensure();
    }

    @Override
    public ContextSnapshot ensure() {
        ContextSnapshot cur = storage.currentOrNull();
        if (cur != null) return cur;

        ContextSnapshot snap = ContextSnapshot.builder()
                .correlationId(UUID.randomUUID().toString())
                .propagated(false)
                .build();
        storage.bind(snap);
        return snap;
    }

    @Override
    public ContextScope scope() {
        ensure();
        return new ContextScope(storage);
    }

    @Override
    public ContextScope scope(ContextSnapshot snapshot) {
        if (snapshot == null) throw new IllegalArgumentException("snapshot must not be null");
        storage.bind(snapshot);
        return new ContextScope(storage);
    }

    @Override
    public Runnable wrap(Runnable runnable) {
        if (runnable == null) throw new IllegalArgumentException("runnable must not be null");
        ContextSnapshot captured = snapshot();
        return () -> {
            try (
            @SuppressWarnings("unused")
			ContextScope scope = scope(captured)) {
                runnable.run();
            }
        };
    }

    @Override
    public <T> Callable<T> wrap(Callable<T> callable) {
        if (callable == null) throw new IllegalArgumentException("callable must not be null");
        ContextSnapshot captured = snapshot();
        return () -> {
            try (
            @SuppressWarnings("unused")
			ContextScope scope = scope(captured)) {
                return callable.call();
            }
        };
    }

    @Override
    public Executor wrap(Executor executor) {
        if (executor == null) throw new IllegalArgumentException("executor must not be null");
        return command -> executor.execute(wrap(command));
    }
}
