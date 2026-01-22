package com.veritynow.core.store.db;

import static com.veritynow.core.store.persistence.jooq.Tables.VN_DIR_ENTRY;
import static com.veritynow.core.store.persistence.jooq.Tables.VN_INODE;
import static com.veritynow.core.store.persistence.jooq.Tables.VN_INODE_PATH_SEGMENT;
import static org.jooq.impl.DSL.currentOffsetDateTime;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Record1;
import org.jooq.Result;
import org.jooq.impl.DSL;

import com.veritynow.core.store.db.jooq.binding.LTree;
import com.veritynow.core.store.db.model.DirEntry;
import com.veritynow.core.store.db.model.Inode;
import com.veritynow.core.store.db.model.InodePathSegment;
import com.veritynow.core.store.persistence.jooq.Indexes;
import com.veritynow.core.store.persistence.jooq.tables.VnInode;
import com.veritynow.core.store.persistence.jooq.tables.records.VnInodeRecord;

/**
 * jOOQ-only replacement for the JPA.
 *
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

    public Optional<Inode> findById(Long id) {
        Objects.requireNonNull(id, "id");

        VnInodeRecord rec = dsl
            .selectFrom(VN_INODE)
            .where(VN_INODE.ID.eq(id))
            .fetchOneInto(VnInodeRecord.class);

        return rec == null ? Optional.empty() : Optional.of(toInode(rec));
    }

    /**
     * Inserts a new inode row and returns the inserted entity with the generated ID.
     */
    public Inode save(Inode entity) {
        Objects.requireNonNull(entity, "entity");
        Objects.requireNonNull(entity.createdAt(), "entity.createdAt");

        OffsetDateTime createdAt = OffsetDateTime.ofInstant(entity.createdAt(), ZoneOffset.UTC);
        LTree scope = entity.scopeKey() == null ? null : LTree.of(entity.scopeKey());

        VnInodeRecord inserted = dsl
            .insertInto(VN_INODE)
            .set(VN_INODE.CREATED_AT, createdAt)
            .set(VN_INODE.SCOPE_KEY, scope)
            .returning(VN_INODE.ID, VN_INODE.CREATED_AT, VN_INODE.SCOPE_KEY)
            .fetchOneInto(VnInodeRecord.class);

        if (inserted == null || inserted.getId() == null) {
            throw new IllegalStateException("Insert into vn_inode did not return an id");
        }

        return toInode(inserted);
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
    
    
    public List<InodePathSegment> findAllByInodeIdOrderByOrdAsc(Long inodeId) {
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

        List<InodePathSegment> out = new ArrayList<>(rows.size());
        for (Record r : rows) {
            out.add(toInodePathSegment(r, I, P, C));
        }
        return out;
    }

    public List<InodePathSegment> saveAll(Iterable<InodePathSegment> entities) {
        Objects.requireNonNull(entities, "entities");

        List<InodePathSegment> list = new ArrayList<>();
        for (InodePathSegment e : entities) {
            Objects.requireNonNull(e, "entity");
            Objects.requireNonNull(e.inode(), "entity.inode");
            Objects.requireNonNull(e.inode().id(), "entity.inode.id");
            Objects.requireNonNull(e.dirEntry(), "entity.dirEntry");
            Objects.requireNonNull(e.dirEntry().id(), "entity.dirEntry.id");
            Objects.requireNonNull(e.createdAt(), "entity.createdAt");

            OffsetDateTime createdAt = OffsetDateTime.ofInstant(e.createdAt(), ZoneOffset.UTC);

            dsl.insertInto(VN_INODE_PATH_SEGMENT)
                .set(VN_INODE_PATH_SEGMENT.INODE_ID, e.inode().id())
                .set(VN_INODE_PATH_SEGMENT.ORD, e.ord())
                .set(VN_INODE_PATH_SEGMENT.DIR_ENTRY_ID, e.dirEntry().id())
                .set(VN_INODE_PATH_SEGMENT.CREATED_AT, createdAt)
                .execute();

            list.add(e);
        }

        return list;
    }

    public Optional<DirEntry> findByParentIdAndName(Long parentInodeId, String name) {
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
            .fetchOptional(r -> toDirEntity(r, P, C));
    }

    public List<DirEntry> findAllByParentIdOrderByNameAsc(Long parentInodeId) {
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
            .fetch(r -> toDirEntity(r, P, C));
    }

    public DirEntry save(DirEntry dir) {
        Objects.requireNonNull(dir, "dir");
        Objects.requireNonNull(dir.parent(), "dir.parent");
        Objects.requireNonNull(dir.child(), "dir.child");
        Objects.requireNonNull(dir.parent().id(), "dir.parentInodeId");
        Objects.requireNonNull(dir.name(), "dir.name");
        Objects.requireNonNull(dir.child().id(), "dir.childInodeId");

        var inserted = dsl
            .insertInto(VN_DIR_ENTRY)
            .set(VN_DIR_ENTRY.PARENT_ID, dir.parent().id())
            .set(VN_DIR_ENTRY.NAME, dir.name())
            .set(VN_DIR_ENTRY.CHILD_ID, dir.child().id())
            .set(VN_DIR_ENTRY.CREATED_AT, currentOffsetDateTime())
            .returning(VN_DIR_ENTRY.ID, VN_DIR_ENTRY.CREATED_AT)
            .fetchOne();

        if (inserted == null || inserted.getId() == null) {
            throw new IllegalStateException("Insert into vn_dir_entry did not return an id");
        }

        return new DirEntry(
            inserted.getId(),
            dir.name(),
            dir.parent(),
            dir.child(),
            inserted.getCreatedAt().toInstant()
        );
    }

    private DirEntry toDirEntity(Record r,
                                   VnInode P,
                                   VnInode C) {

        if (r.get(P.CREATED_AT) == null || r.get(C.CREATED_AT) == null) {
            throw new IllegalStateException("vn_inode.created_at is NULL (violates schema invariant)");
        }

        String pScope = r.get(P.SCOPE_KEY) == null ? null : r.get(P.SCOPE_KEY).toString();
        String cScope = r.get(C.SCOPE_KEY) == null ? null : r.get(C.SCOPE_KEY).toString();

        Inode parent = new Inode(r.get(P.ID), r.get(P.CREATED_AT).toInstant(), pScope);
        Inode child  = new Inode(r.get(C.ID), r.get(C.CREATED_AT).toInstant(), cScope);

        if (r.get(VN_DIR_ENTRY.CREATED_AT) == null) {
            throw new IllegalStateException("vn_dir_entry.created_at is NULL (violates schema invariant)");
        }

        return new DirEntry(
            r.get(VN_DIR_ENTRY.ID),
            r.get(VN_DIR_ENTRY.NAME),
            parent,
            child,
            r.get(VN_DIR_ENTRY.CREATED_AT).toInstant()
        );
    }
    
    
    private InodePathSegment toInodePathSegment(
        Record r,
        VnInode I,
        VnInode P,
        VnInode C
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

        Inode inode = new Inode(r.get(I.ID), r.get(I.CREATED_AT).toInstant(), iScope);
        Inode parent = new Inode(r.get(P.ID), r.get(P.CREATED_AT).toInstant(), pScope);
        Inode child  = new Inode(r.get(C.ID), r.get(C.CREATED_AT).toInstant(), cScope);

        DirEntry dirEntry = new DirEntry(
            r.get(VN_DIR_ENTRY.ID),
            r.get(VN_DIR_ENTRY.NAME),
            parent,
            child,
            r.get(VN_DIR_ENTRY.CREATED_AT).toInstant()
        );

        Instant segCreatedAt = r.get(VN_INODE_PATH_SEGMENT.CREATED_AT).toInstant();

        return new InodePathSegment(
            r.get(VN_INODE_PATH_SEGMENT.ID),
            inode,
            r.get(VN_INODE_PATH_SEGMENT.ORD),
            dirEntry,
            segCreatedAt
        );
    }
    
        private static Inode toInode(VnInodeRecord r) {
        if (r.getCreatedAt() == null) {
            throw new IllegalStateException("vn_inode.created_at is NULL (violates schema invariant)");
        }
        Instant createdAt = r.getCreatedAt().toInstant();
        String scopeKey = r.getScopeKey() == null ? null : r.getScopeKey().toString();
        return new Inode(r.getId(), createdAt, scopeKey);
    }
}
