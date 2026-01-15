package com.veritynow.v2.txn.adapters.memory;

import com.veritynow.v2.txn.spi.Clock;
import com.veritynow.v2.txn.spi.LockHandle;
import com.veritynow.v2.txn.spi.SubtreeLockService;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * In-memory subtree lock service (lease-based) for zero-infra development and unit tests.
 *
 * Semantics:
 *  - EXCLUSIVE subtree locks only (prefix overlap conflict).
 *  - TTL/lease ensures locks expire if the holder disappears.
 *  - Fencing token is monotonic in-process only.
 *
 * Not suitable for multi-node deployments.
 */
public class InMemorySubtreeLockService implements SubtreeLockService {

    private final Clock clock;
    private final AtomicLong tokenSeq = new AtomicLong(0);
    private final Map<String, LockHandle> locksById = new ConcurrentHashMap<>();

    public InMemorySubtreeLockService(Clock clock) {
        this.clock = clock;
    }

    @Override
    public synchronized LockHandle acquire(String lockRoot, String ownerTxnId, long ttlMs) {
        String root = normalize(lockRoot);
        long now = clock.nowMs();
        cleanupExpired(now);

        // Prefix overlap check
        for (LockHandle l : locksById.values()) {
            if (conflicts(root, l.lockRoot())) {
                throw new IllegalStateException("Lock conflict: requested=" + root + " existing=" + l.lockRoot());
            }
        }

        String lockId = "mem-" + UUID.randomUUID();
        long expiresAt = now + Math.max(1, ttlMs);
        long token = tokenSeq.incrementAndGet();

        LockHandle handle = new LockHandle(lockId, root, ownerTxnId, expiresAt, token);
        locksById.put(lockId, handle);
        return handle;
    }

    @Override
    public synchronized LockHandle renew(String lockId) {
        long now = clock.nowMs();
        cleanupExpired(now);

        LockHandle existing = locksById.get(lockId);
        if (existing == null) {
            throw new IllegalStateException("Lock not found or expired: " + lockId);
        }

        // Keep same token; extend expiry by the original TTL heuristic (here: +60s default)
        long expiresAt = now + 60_000;
        LockHandle renewed = new LockHandle(existing.lockId(), existing.lockRoot(), existing.ownerTxnId(), expiresAt, existing.fencingToken());
        locksById.put(lockId, renewed);
        return renewed;
    }

    @Override
    public synchronized void release(String lockId) {
        locksById.remove(lockId);
    }

    private void cleanupExpired(long now) {
        locksById.entrySet().removeIf(e -> e.getValue().expiresAtMs() <= now);
    }

    private static boolean conflicts(String a, String b) {
        return isPrefixOf(a, b) || isPrefixOf(b, a);
    }

    private static boolean isPrefixOf(String prefix, String path) {
        if (prefix.equals("/")) return true;
        if (path.equals(prefix)) return true;
        return path.startsWith(prefix + "/");
    }

    private static String normalize(String p) {
        if (p == null || p.isBlank()) return "/";
        String x = p.trim();
        if (!x.startsWith("/")) x = "/" + x;
        if (x.length() > 1 && x.endsWith("/")) x = x.substring(0, x.length() - 1);
        return x;
    }
}
