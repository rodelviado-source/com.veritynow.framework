package com.veritynow.v2.txn.adapters.jpa;

import com.veritynow.v2.txn.spi.LockHandle;
import com.veritynow.v2.txn.spi.SubtreeLockService;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * DB-backed lease lock with subtree (prefix) overlap checks.
 *
 * Enterprise note: for very high lock concurrency, replace overlap scanning with a more sophisticated index
 * (e.g., materialized path segments, ltree, or Redis/etcd). In practice, active lock count is low.
 */
public class DbSubtreeLockService implements SubtreeLockService {
    private final JpaSubtreeLockRepository repo;
    private final FencingTokenProvider fencingTokenProvider;
    private final com.veritynow.v2.txn.spi.Clock clock;

    public DbSubtreeLockService(JpaSubtreeLockRepository repo, FencingTokenProvider fencingTokenProvider, com.veritynow.v2.txn.spi.Clock clock) {
        this.repo = Objects.requireNonNull(repo);
        this.fencingTokenProvider = Objects.requireNonNull(fencingTokenProvider);
        this.clock = Objects.requireNonNull(clock);
    }

    @Override
    @Transactional
    public LockHandle acquire(String lockRoot, String ownerTxnId, long ttlMs) {
        Objects.requireNonNull(lockRoot);
        Objects.requireNonNull(ownerTxnId);
        String norm = normalize(lockRoot);
        long now = clock.nowMs();
        long expires = now + Math.max(1_000L, ttlMs);

        // Reject if any active lock overlaps
        List<SubtreeLockEntity> active = repo.findActive(now);
        for (SubtreeLockEntity l : active) {
            if (overlaps(norm, l.getLockRoot())) {
                throw new IllegalStateException("Lock conflict: requested=" + norm + " overlaps existing=" + l.getLockRoot());
            }
        }

        String lockId = UUID.randomUUID().toString();
        long token = fencingTokenProvider.nextToken();

        SubtreeLockEntity ent = new SubtreeLockEntity(lockId, norm, ownerTxnId, expires, token);
        repo.save(ent);

        return new LockHandle(lockId, norm, ownerTxnId, expires, token);
    }

    @Override
    @Transactional
    public LockHandle renew(String lockId) {
        Objects.requireNonNull(lockId);
        long now = clock.nowMs();
        SubtreeLockEntity l = repo.findById(lockId).orElseThrow(() -> new IllegalStateException("Lock not found: " + lockId));
        if (l.getExpiresAtMs() <= now) {
            throw new IllegalStateException("Lock expired: " + lockId);
        }
        // Extend by the remaining TTL or a default (server-driven). Default: 60s.
        long newExpires = now + 60_000L;
        l.setExpiresAtMs(newExpires);
        repo.save(l);
        return new LockHandle(l.getLockId(), l.getLockRoot(), l.getOwnerTxnId(), l.getExpiresAtMs(), l.getFencingToken());
    }

    @Override
    @Transactional
    public void release(String lockId) {
        Objects.requireNonNull(lockId);
        repo.deleteByLockId(lockId);
    }

    private static String normalize(String p) {
        String x = p.trim();
        if (x.endsWith("/")) x = x.substring(0, x.length()-1);
        if (x.isEmpty()) x = "/";
        return x;
    }

    private static boolean overlaps(String a, String b) {
        String na = normalize(a);
        String nb = normalize(b);
        if (na.equals("/")) return true;
        if (nb.equals("/")) return true;
        return isPrefix(na, nb) || isPrefix(nb, na);
    }

    private static boolean isPrefix(String root, String other) {
        if (root.equals(other)) return true;
        return other.startsWith(root + "/");
    }
}
