package com.veritynow.context;

/**
 * In-process context storage (default: ThreadLocal).
 */
public interface ContextStorage {
    ContextSnapshot currentOrNull();
    void bind(ContextSnapshot snapshot);
    void clear();
}
