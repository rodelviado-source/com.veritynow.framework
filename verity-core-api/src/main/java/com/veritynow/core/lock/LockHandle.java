package com.veritynow.core.lock;

import java.util.UUID;

/**
 * Minimal lock handle.
 *
 * Contains only what is required to:
 * - release the lock group
 * - enforce fencing at publish/HEAD-move time
 */
public record LockHandle(
    String ownerId,
    UUID lockGroupId,
    long fenceToken
) {}
