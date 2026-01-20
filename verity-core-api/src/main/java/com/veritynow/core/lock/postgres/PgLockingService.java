package com.veritynow.core.lock.postgres;

import java.sql.Array;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.veritynow.core.context.Context;
import com.veritynow.core.context.ContextSnapshot;
import com.veritynow.core.lock.LockHandle;
import com.veritynow.core.lock.LockingService;
import com.veritynow.core.store.jpa.PathKeyCodec;
import com.veritynow.core.store.jpa.PathUtils;

/**
 * Postgres locking kernel: exclusive subtree locking using ltree scope keys.
 *
 * Alignment rules:
 * - Locking does not compute segment hashes or ltree codecs.
 * - Store provides precomputed ltree scope keys (per-inode) for requested paths.
 */
@Service
public class PgLockingService implements LockingService {

    private final JdbcTemplate jdbc;
    

    public PgLockingService(JdbcTemplate jdbc) {
        this.jdbc = Objects.requireNonNull(jdbc, "JDBC required");
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.SERIALIZABLE)
    public LockHandle acquire(List<String> paths) {
        Objects.requireNonNull(paths, "paths");

        ContextSnapshot snap = Context.snapshot();
        String ownerId = snap.transactionIdOrNull();
        
        if (ownerId == null || ownerId.isBlank()) {
            ownerId = snap.correlationId();
        }
        if (ownerId == null || ownerId.isBlank()) {
            throw new IllegalStateException("No transactionId/correlationId in Context");
        }

        List<String> normalized = paths.stream()
                .filter(Objects::nonNull)
                .map(PathUtils::normalizePath)
                .distinct()
                .sorted()
                .toList();

        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("No scopes provided");
        }

        List<String> minimalScopes = minimizeScopes(normalized);

        // Resolve ltree scope keys using store-owned codec logic. The inode may not exist yet
        // (e.g., callers lock an intent path before creation), so we must not require resolution
        // through the inode/direntry graph here.
        List<String> scopeKeys = minimalScopes.stream()
                .map(PathKeyCodec::toLTree)
                .toList();

        // 1) Conflict check
        if (existsConflicts(ownerId, scopeKeys)) {
            throw new IllegalStateException("Lock conflict");
        }

        // 2) Allocate fence token
        long fenceToken = nextFenceToken();

        // 3) Create lock group
        UUID lockGroupId = UUID.randomUUID();
        insertLockGroup(lockGroupId, ownerId, fenceToken);

        // 4) Insert lock rows (scope keys already derived by store)
        insertPathLocks(lockGroupId, ownerId, scopeKeys);

        // 5) For transparency/debugging, capture derived scope keys as text
        //List<String> scopeKeys = resolveScopeKeys(minimalScopes);

        return new LockHandle(ownerId, lockGroupId, fenceToken);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void release(LockHandle handle) {
        if (handle == null) return;

        jdbc.update(
            "UPDATE vn_path_lock " +
            "SET active = false, released_at = now() " +
            "WHERE lock_group_id = ? AND active = true",
            handle.lockGroupId()
        );

        jdbc.update(
            "UPDATE vn_lock_group " +
            "SET active = false, released_at = now() " +
            "WHERE lock_group_id = ? AND active = true",
            handle.lockGroupId()
        );
    }

    private boolean existsConflicts(String ownerId, List<String> scopeKeys) {
        Integer v = jdbc.query(con -> {
            PreparedStatement ps = con.prepareStatement(
                // Compute candidate ltree[] once, then use for overlap predicates.
                "WITH keys AS (" +
                "  SELECT array_agg(p::ltree) AS ks " +
                "  FROM unnest(?::text[]) AS p" +
                ") " +
                "SELECT CASE WHEN EXISTS (" +
                "  SELECT 1 " +
                "  FROM vn_path_lock pl " +
                "  JOIN vn_lock_group lg ON lg.lock_group_id = pl.lock_group_id " +
                "  CROSS JOIN keys " +
                "  WHERE pl.active = true " +
                "    AND lg.active = true " +
                "    AND pl.owner_id <> ? " +
                "    AND ( " +
                "      pl.scope_key @> ANY (keys.ks) " +
                "      OR pl.scope_key <@ ANY (keys.ks) " +
                "    )" +
                ") THEN 1 ELSE 0 END"
            );

            Array arr = con.createArrayOf("text", scopeKeys.toArray(String[]::new));
            ps.setArray(1, arr);
            ps.setString(2, ownerId);
            return ps;
        }, rs -> rs.next() ? rs.getInt(1) : 0);

        return v != null && v.intValue() == 1;
    }

    private long nextFenceToken() {
        Long v = jdbc.queryForObject("select nextval('vn_fence_token_seq')", Long.class);
        if (v == null) throw new IllegalStateException("Failed to allocate fence token");
        return v;
    }

    private void insertLockGroup(UUID lockGroupId, String ownerId, long fenceToken) {
        jdbc.update(
            "INSERT INTO vn_lock_group(lock_group_id, owner_id, fence_token, active, acquired_at) " +
            "VALUES (?, ?, ?, true, now())",
            lockGroupId, ownerId, fenceToken
        );
    }

    private void insertPathLocks(UUID lockGroupId, String ownerId, List<String> scopeKeys) {
        // Insert one lock row per scope. scope_key is already derived by the store.
        jdbc.batchUpdate(
            "INSERT INTO vn_path_lock(lock_group_id, owner_id, scope_key, active, acquired_at) " +
            "VALUES (?, ?, ?::ltree, true, now())",
            scopeKeys,
            200,
            (ps, scope) -> {
                ps.setObject(1, lockGroupId);
                ps.setString(2, ownerId);
                ps.setString(3, scope);
            }
        );
    }

    private static List<String> minimizeScopes(List<String> sortedScopes) {
        List<String> out = new ArrayList<>();
        for (String s : sortedScopes) {
            boolean covered = false;
            for (String kept : out) {
                if (isAncestorOrSame(kept, s)) {
                    covered = true;
                    break;
                }
            }
            if (!covered) out.add(s);
        }
        return out;
    }

    private static boolean isAncestorOrSame(String a, String b) {
        if (a.equals(b)) return true;
        if ("/".equals(a)) return true;
        return b.startsWith(a + "/");
    }
    
    
}
