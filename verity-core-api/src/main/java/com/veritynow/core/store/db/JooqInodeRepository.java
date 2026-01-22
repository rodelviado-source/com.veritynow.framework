package com.veritynow.core.store.db;

import static com.veritynow.core.store.persistence.jooq.Tables.VN_INODE;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.Optional;

import org.jooq.DSLContext;
import org.jooq.Record1;
import org.jooq.impl.DSL;

import com.veritynow.core.store.db.jooq.binding.LTree;
import com.veritynow.core.store.persistence.jooq.Indexes;
import com.veritynow.core.store.persistence.jooq.tables.records.VnInodeRecord;

/**
 * jOOQ-only replacement for the JPA {@code InodeRepository}.
 *
 * <p>Semantics preserved for the callers (notably {@link InodeManager}):
 * <ul>
 *   <li>{@code save(..)} performs an INSERT and returns an {@link InodeEntity} with the DB identity populated</li>
 *   <li>{@code findById(..)} returns the inode row if present</li>
 *   <li>{@code findIdByScopeKey(..)} performs an equality lookup on {@code scope_key}</li>
 *   <li>{@code flush()} is a no-op (jOOQ executes immediately)</li>
 * </ul>
 */
public final class JooqInodeRepository {

    private final DSLContext dsl;

    public JooqInodeRepository(DSLContext dsl) {
        this.dsl = Objects.requireNonNull(dsl, "dsl");
    }

    /**
     * Equivalent to the legacy native query:
     * {@code select id from vn_inode where scope_key = cast(? as ltree)}
     */
    public Optional<Long> findIdByScopeKey(String scopeKey) {
        Objects.requireNonNull(scopeKey, "scopeKey");

        Record1<Long> r = dsl
            .select(VN_INODE.ID)
            .from(VN_INODE)
            .where(DSL.condition("{0} = cast({1} as ltree)", VN_INODE.SCOPE_KEY, DSL.val(scopeKey)))
            .fetchOne();

        return r == null ? Optional.empty() : Optional.ofNullable(r.value1());
    }

    public Optional<InodeEntity> findById(Long id) {
        Objects.requireNonNull(id, "id");

        VnInodeRecord rec = dsl
            .selectFrom(VN_INODE)
            .where(VN_INODE.ID.eq(id))
            .fetchOneInto(VnInodeRecord.class);

        return rec == null ? Optional.empty() : Optional.of(toEntity(rec));
    }

    /**
     * Inserts a new inode row and returns the inserted entity with the generated ID.
     */
    public InodeEntity save(InodeEntity entity) {
        Objects.requireNonNull(entity, "entity");
        Objects.requireNonNull(entity.getCreatedAt(), "entity.createdAt");

        OffsetDateTime createdAt = OffsetDateTime.ofInstant(entity.getCreatedAt(), ZoneOffset.UTC);
        LTree scope = entity.getScopeKey() == null ? null : LTree.of(entity.getScopeKey());

        VnInodeRecord inserted = dsl
            .insertInto(VN_INODE)
            .set(VN_INODE.CREATED_AT, createdAt)
            .set(VN_INODE.SCOPE_KEY, scope)
            .returning(VN_INODE.ID, VN_INODE.CREATED_AT, VN_INODE.SCOPE_KEY)
            .fetchOneInto(VnInodeRecord.class);

        if (inserted == null || inserted.getId() == null) {
            throw new IllegalStateException("Insert into vn_inode did not return an id");
        }

        return toEntity(inserted);
    }
    
    public Optional<Long> resolveInodeId(String nodePath) {
		Objects.requireNonNull(nodePath, "nodePath");
		String scopeKey = PathKeyCodec.toLTree(nodePath);
		return findIdByScopeKey(scopeKey);
    }


    public void ensureScopeKeyUniqueIndex() {
        dsl.createUniqueIndexIfNotExists(Indexes.UQ_VN_INODE_SCOPE_KEY.getName())
           .on(VN_INODE, VN_INODE.SCOPE_KEY)
           .execute();
    }
    
    
        private static InodeEntity toEntity(VnInodeRecord r) {
        if (r.getCreatedAt() == null) {
            throw new IllegalStateException("vn_inode.created_at is NULL (violates schema invariant)");
        }
        Instant createdAt = r.getCreatedAt().toInstant();
        String scopeKey = r.getScopeKey() == null ? null : r.getScopeKey().toString();
        return new InodeEntity(r.getId(), createdAt, scopeKey);
    }
}
