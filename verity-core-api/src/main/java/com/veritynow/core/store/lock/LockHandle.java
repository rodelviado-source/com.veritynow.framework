package com.veritynow.core.store.lock;

import com.veritynow.core.context.ContextScope;

/**
 * Minimal lock handle.
 *
 * Contains only what is required to:
 * - release the lock context scope   
 * Lock enforcement is handled directly by the DB
 */
public record LockHandle (
    String tnxId,
    ContextScope scope
) {}
