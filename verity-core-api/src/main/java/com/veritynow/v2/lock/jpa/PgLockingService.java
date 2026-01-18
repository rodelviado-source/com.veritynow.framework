package com.veritynow.v2.lock.jpa;

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

import com.veritynow.context.Context;
import com.veritynow.context.ContextSnapshot;
import com.veritynow.v2.lock.LockHandle;
import com.veritynow.v2.lock.LockingService;
import com.veritynow.v2.store.core.jpa.PathUtils;

/**
 * Postgres locking kernel: exclusive subtree locking using ltree scope keys.
 *
 * Alignment rules:
 * - No Java codec (PathKeyCodec removed).
 * - ltree scope keys are derived in Postgres via vn_path_to_scope_key(text).
 * - Store remains agnostic; callers pass normalized absolute paths for scopes.
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

        // 1) Conflict check (derive candidate scope keys inside Postgres)
        if (existsConflicts(ownerId, minimalScopes)) {
            throw new IllegalStateException("Lock conflict");
        }

        // 2) Allocate fence token
        long fenceToken = nextFenceToken();

        // 3) Create lock group
        UUID lockGroupId = UUID.randomUUID();
        insertLockGroup(lockGroupId, ownerId, fenceToken);

        // 4) Insert lock rows with Postgres-derived scope keys
        insertPathLocks(lockGroupId, ownerId, minimalScopes);

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

    private boolean existsConflicts(String ownerId, List<String> scopes) {
        Integer v = jdbc.query(con -> {
            PreparedStatement ps = con.prepareStatement(
                // Compute candidate ltree[] once, then use for overlap predicates.
                "WITH keys AS (" +
                "  SELECT array_agg(vn_path_to_scope_key(p)) AS ks " +
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

            Array arr = con.createArrayOf("text", scopes.toArray(String[]::new));
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

    private void insertPathLocks(UUID lockGroupId, String ownerId, List<String> scopes) {
        // Insert one lock row per scope. scope_key is derived in Postgres.
        jdbc.batchUpdate(
            "INSERT INTO vn_path_lock(lock_group_id, owner_id, scope_key, active, acquired_at) " +
            "VALUES (?, ?, vn_path_to_scope_key(?), true, now())",
            scopes,
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
