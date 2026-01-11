package com.veritynow.context;

import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;

/**
 * Default ContextManager implementation.
 */
final class DefaultContextManager implements ContextManager {

    private final ContextStorage storage;
    private final IdGenerator idGenerator;
    private final MdcBridge mdcBridge;

    DefaultContextManager(ContextStorage storage, IdGenerator idGenerator, MdcBridge mdcBridge) {
        this.storage = storage;
        this.idGenerator = idGenerator;
        this.mdcBridge = mdcBridge;
    }

    @Override
    public String getCorrelationId() {
        return ensure().correlationId();
    }

    @Override
    public Optional<String> getTransactionId() {
        return Optional.ofNullable(ensure().transactionIdOrNull());
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
    public ContextSnapshot snapshot() {
        return ensure();
    }

    @Override
    public ContextSnapshot ensure() {
        ContextSnapshot cur = storage.currentOrNull();
        if (cur != null) return cur;

        // If MDC is present and already has a trace id, adopt it.
        if (mdcBridge != null) {
            Optional<ContextSnapshot> opt = mdcBridge.tryExtract();
            if (opt.isPresent()) {
                ContextSnapshot snap = opt.get();
                storage.bind(snap);
                mdcBridge.apply(snap);
                return snap;
            }
        }

        ContextSnapshot snap = ContextSnapshot.builder()
                .correlationId(idGenerator.newCorrelationId())
                .propagated(false)
                .build();
        storage.bind(snap);
        if (mdcBridge != null) mdcBridge.apply(snap);
        return snap;
    }

    @Override
    public ContextScope scope() {
        ensure();
        ContextSnapshot prev = storage.currentOrNull();
        return new ContextScope(storage, prev, mdcBridge);
    }

    @Override
    public ContextScope scope(ContextSnapshot snapshot) {
        if (snapshot == null) throw new IllegalArgumentException("snapshot must not be null");
        ContextSnapshot prev = storage.currentOrNull();
        storage.bind(snapshot);
        if (mdcBridge != null) mdcBridge.apply(snapshot);
        return new ContextScope(storage, prev, mdcBridge);
    }

    @Override
    public Runnable wrap(Runnable runnable) {
        if (runnable == null) throw new IllegalArgumentException("runnable must not be null");
        ContextSnapshot captured = snapshot();
        return () -> {
            try (ContextScope scope = scope(captured)) {
                runnable.run();
            }
        };
    }

    @Override
    public <T> Callable<T> wrap(Callable<T> callable) {
        if (callable == null) throw new IllegalArgumentException("callable must not be null");
        ContextSnapshot captured = snapshot();
        return () -> {
            try (ContextScope scope = scope(captured)) {
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
