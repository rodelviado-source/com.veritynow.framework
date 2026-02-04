package com.veritynow.core.store.lock;

import com.veritynow.core.context.ContextScope;

/**
 * Minimal lock handle.
 *
 * Contains only what is required to:
 * - release the lock group
 * - enforce fencing at publish/HEAD-move time
 */
public record LockHandle (
    String ownerId,
    ContextScope scope
) {}
