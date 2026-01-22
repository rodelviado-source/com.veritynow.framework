package com.veritynow.core.store.db;

import static com.veritynow.core.store.persistence.jooq.Tables.VN_DIR_ENTRY;
import static com.veritynow.core.store.persistence.jooq.Tables.VN_INODE;
import static com.veritynow.core.store.persistence.jooq.Tables.VN_INODE_PATH_SEGMENT;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;

/**
 * jOOQ implementation of {@link InodePathSegmentRepository}.
 *
 * This repository must faithfully materialize stored rows (no synthesized timestamps/fields).
 */
public final class JooqInodePathSegmentRepository  {

    private final DSLContext dsl;

    public JooqInodePathSegmentRepository(DSLContext dsl) {
        this.dsl = Objects.requireNonNull(dsl, "dsl");
    }

    public List<InodePathSegmentEntity> findAllByInode_IdOrderByOrdAsc(Long inodeId) {
        Objects.requireNonNull(inodeId, "inodeId");

        var I = VN_INODE.as("i"); // owning inode of the segment
        var P = VN_INODE.as("p"); // parent inode for dir entry
        var C = VN_INODE.as("c"); // child inode for dir entry

        Result<Record> rows = dsl
            .select(VN_INODE_PATH_SEGMENT.fields())
            .select(VN_DIR_ENTRY.fields())
            .select(I.fields())
            .select(P.fields())
            .select(C.fields())
            .from(VN_INODE_PATH_SEGMENT)
            .join(VN_DIR_ENTRY).on(VN_DIR_ENTRY.ID.eq(VN_INODE_PATH_SEGMENT.DIR_ENTRY_ID))
            .join(I).on(I.ID.eq(VN_INODE_PATH_SEGMENT.INODE_ID))
            .join(P).on(P.ID.eq(VN_DIR_ENTRY.PARENT_ID))
            .join(C).on(C.ID.eq(VN_DIR_ENTRY.CHILD_ID))
            .where(VN_INODE_PATH_SEGMENT.INODE_ID.eq(inodeId))
            .orderBy(VN_INODE_PATH_SEGMENT.ORD.asc())
            .fetch();

        List<InodePathSegmentEntity> out = new ArrayList<>(rows.size());
        for (Record r : rows) {
            out.add(toEntity(r, I, P, C));
        }
        return out;
    }

    public List<InodePathSegmentEntity> saveAll(Iterable<InodePathSegmentEntity> entities) {
        Objects.requireNonNull(entities, "entities");

        List<InodePathSegmentEntity> list = new ArrayList<>();
        for (InodePathSegmentEntity e : entities) {
            Objects.requireNonNull(e, "entity");
            Objects.requireNonNull(e.getInode(), "entity.inode");
            Objects.requireNonNull(e.getInode().getId(), "entity.inode.id");
            Objects.requireNonNull(e.getDirEntry(), "entity.dirEntry");
            Objects.requireNonNull(e.getDirEntry().getId(), "entity.dirEntry.id");
            Objects.requireNonNull(e.getCreatedAt(), "entity.createdAt");

            OffsetDateTime createdAt = OffsetDateTime.ofInstant(e.getCreatedAt(), ZoneOffset.UTC);

            dsl.insertInto(VN_INODE_PATH_SEGMENT)
                .set(VN_INODE_PATH_SEGMENT.INODE_ID, e.getInode().getId())
                .set(VN_INODE_PATH_SEGMENT.ORD, e.getOrd())
                .set(VN_INODE_PATH_SEGMENT.DIR_ENTRY_ID, e.getDirEntry().getId())
                .set(VN_INODE_PATH_SEGMENT.CREATED_AT, createdAt)
                .execute();

            list.add(e);
        }

        return list;
    }

    private InodePathSegmentEntity toEntity(
        Record r,
        com.veritynow.core.store.persistence.jooq.tables.VnInode I,
        com.veritynow.core.store.persistence.jooq.tables.VnInode P,
        com.veritynow.core.store.persistence.jooq.tables.VnInode C
    ) {

        if (r.get(I.CREATED_AT) == null || r.get(P.CREATED_AT) == null || r.get(C.CREATED_AT) == null) {
            throw new IllegalStateException("vn_inode.created_at is NULL (violates schema invariant)");
        }
        if (r.get(VN_DIR_ENTRY.CREATED_AT) == null) {
            throw new IllegalStateException("vn_dir_entry.created_at is NULL (violates schema invariant)");
        }
        if (r.get(VN_INODE_PATH_SEGMENT.CREATED_AT) == null) {
            throw new IllegalStateException("vn_inode_path_segment.created_at is NULL (violates schema invariant)");
        }

        String iScope = r.get(I.SCOPE_KEY) == null ? null : r.get(I.SCOPE_KEY).toString();
        String pScope = r.get(P.SCOPE_KEY) == null ? null : r.get(P.SCOPE_KEY).toString();
        String cScope = r.get(C.SCOPE_KEY) == null ? null : r.get(C.SCOPE_KEY).toString();

        InodeEntity inode = new InodeEntity(r.get(I.ID), r.get(I.CREATED_AT).toInstant(), iScope);
        InodeEntity parent = new InodeEntity(r.get(P.ID), r.get(P.CREATED_AT).toInstant(), pScope);
        InodeEntity child  = new InodeEntity(r.get(C.ID), r.get(C.CREATED_AT).toInstant(), cScope);

        DirEntryEntity dirEntry = new DirEntryEntity(
            r.get(VN_DIR_ENTRY.ID),
            parent,
            r.get(VN_DIR_ENTRY.NAME),
            child,
            r.get(VN_DIR_ENTRY.CREATED_AT).toInstant()
        );

        Instant segCreatedAt = r.get(VN_INODE_PATH_SEGMENT.CREATED_AT).toInstant();

        return new InodePathSegmentEntity(
            r.get(VN_INODE_PATH_SEGMENT.ID),
            inode,
            r.get(VN_INODE_PATH_SEGMENT.ORD),
            dirEntry,
            segCreatedAt
        );
    }
}
