package com.veritynow.core.store.db;

import static com.veritynow.core.store.persistence.jooq.Tables.VN_DIR_ENTRY;
import static com.veritynow.core.store.persistence.jooq.Tables.VN_INODE;
import static org.jooq.impl.DSL.currentOffsetDateTime;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.jooq.DSLContext;
import org.jooq.Record;

public final class JooqDirEntryRepository  {

    private final DSLContext dsl;

    public JooqDirEntryRepository(DSLContext dsl) {
        this.dsl = Objects.requireNonNull(dsl, "dsl");
    }

    public Optional<DirEntryEntity> findByParentIdAndName(Long parentInodeId, String name) {
        Objects.requireNonNull(parentInodeId, "parentInodeId");
        Objects.requireNonNull(name, "name");

        var P = VN_INODE.as("p");
        var C = VN_INODE.as("c");

        return dsl
            .select(VN_DIR_ENTRY.fields())
            .select(P.fields())
            .select(C.fields())
            .from(VN_DIR_ENTRY)
            .join(P).on(P.ID.eq(VN_DIR_ENTRY.PARENT_ID))
            .join(C).on(C.ID.eq(VN_DIR_ENTRY.CHILD_ID))
            .where(
                VN_DIR_ENTRY.PARENT_ID.eq(parentInodeId)
                    .and(VN_DIR_ENTRY.NAME.eq(name))
            )
            .fetchOptional(r -> toEntity(r, P, C));
    }

    public List<DirEntryEntity> findAllByParentIdOrderByNameAsc(Long parentInodeId) {
        Objects.requireNonNull(parentInodeId, "parentInodeId");

        var P = VN_INODE.as("p");
        var C = VN_INODE.as("c");

        return dsl
            .select(VN_DIR_ENTRY.fields())
            .select(P.fields())
            .select(C.fields())
            .from(VN_DIR_ENTRY)
            .join(P).on(P.ID.eq(VN_DIR_ENTRY.PARENT_ID))
            .join(C).on(C.ID.eq(VN_DIR_ENTRY.CHILD_ID))
            .where(VN_DIR_ENTRY.PARENT_ID.eq(parentInodeId))
            .orderBy(VN_DIR_ENTRY.NAME.asc())
            .fetch(r -> toEntity(r, P, C));
    }

    public DirEntryEntity save(DirEntryEntity entity) {
        Objects.requireNonNull(entity, "entity");
        Objects.requireNonNull(entity.getParent(), "entity.parent");
        Objects.requireNonNull(entity.getChild(), "entity.child");
        Objects.requireNonNull(entity.getParent().getId(), "entity.parentInodeId");
        Objects.requireNonNull(entity.getName(), "entity.name");
        Objects.requireNonNull(entity.getChild().getId(), "entity.childInodeId");

        var inserted = dsl
            .insertInto(VN_DIR_ENTRY)
            .set(VN_DIR_ENTRY.PARENT_ID, entity.getParent().getId())
            .set(VN_DIR_ENTRY.NAME, entity.getName())
            .set(VN_DIR_ENTRY.CHILD_ID, entity.getChild().getId())
            .set(VN_DIR_ENTRY.CREATED_AT, currentOffsetDateTime())
            .returning(VN_DIR_ENTRY.ID, VN_DIR_ENTRY.CREATED_AT)
            .fetchOne();

        if (inserted == null || inserted.getId() == null) {
            throw new IllegalStateException("Insert into vn_dir_entry did not return an id");
        }

        return new DirEntryEntity(
            inserted.getId(),
            entity.getParent(),
            entity.getName(),
            entity.getChild(),
            inserted.getCreatedAt().toInstant()
        );
    }

    private DirEntryEntity toEntity(Record r,
                                   com.veritynow.core.store.persistence.jooq.tables.VnInode P,
                                   com.veritynow.core.store.persistence.jooq.tables.VnInode C) {

        if (r.get(P.CREATED_AT) == null || r.get(C.CREATED_AT) == null) {
            throw new IllegalStateException("vn_inode.created_at is NULL (violates schema invariant)");
        }

        String pScope = r.get(P.SCOPE_KEY) == null ? null : r.get(P.SCOPE_KEY).toString();
        String cScope = r.get(C.SCOPE_KEY) == null ? null : r.get(C.SCOPE_KEY).toString();

        InodeEntity parent = new InodeEntity(r.get(P.ID), r.get(P.CREATED_AT).toInstant(), pScope);
        InodeEntity child  = new InodeEntity(r.get(C.ID), r.get(C.CREATED_AT).toInstant(), cScope);

        if (r.get(VN_DIR_ENTRY.CREATED_AT) == null) {
            throw new IllegalStateException("vn_dir_entry.created_at is NULL (violates schema invariant)");
        }

        return new DirEntryEntity(
            r.get(VN_DIR_ENTRY.ID),
            parent,
            r.get(VN_DIR_ENTRY.NAME),
            child,
            r.get(VN_DIR_ENTRY.CREATED_AT).toInstant()
        );
    }
}
