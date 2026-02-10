package com.veritynow.core.store.versionstore.repo;

import static com.veritynow.core.store.persistence.jooq.Tables.VN_DIR_ENTRY;
import static com.veritynow.core.store.persistence.jooq.Tables.VN_INODE;
import static com.veritynow.core.store.persistence.jooq.Tables.VN_INODE_PATH_SEGMENT;
import static org.jooq.impl.DSL.condition;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Param;
import org.jooq.Record;
import org.jooq.Record1;
import org.jooq.Result;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.jooq.postgres.extensions.types.Ltree;

import com.veritynow.core.context.Context;
import com.veritynow.core.store.persistence.jooq.tables.VnInode;
import com.veritynow.core.store.persistence.jooq.tables.records.VnInodeRecord;
import com.veritynow.core.store.txn.TransactionContext;
import com.veritynow.core.store.versionstore.PathUtils;
import com.veritynow.core.store.versionstore.model.DirEntry;
import com.veritynow.core.store.versionstore.model.Inode;
import com.veritynow.core.store.versionstore.model.InodePathSegment;

/**
 * jOOQ-only replacement for the JPA.
 *
 */
public final class InodeRepository {

    private final DSLContext defaultDSL;
    private static final Logger LOGGER = LogManager.getLogger();
    
    public InodeRepository(DSLContext dsl) {
        this.defaultDSL = dsl;
    }

    private DSLContext ensureDSL() {
    	if (!Context.isActive()) {
    		return defaultDSL;
    	}
    	String txnId = Context.transactionIdOrNull();
    	if (txnId == null) {
    		return defaultDSL;
    	}
    	Connection conn = TransactionContext.getConnection(txnId);
    	if (conn == null) {
    		return defaultDSL;
    	}
    	
   		return DSL.using(conn, SQLDialect.POSTGRES);
    	
    }
    /**
     * Equivalent to the legacy native query:
     * {@code select id from vn_inode where scope_key = cast(? as ltree)}
     */
    public Optional<Long> findIdByScopeKey(String scopeKey) {
        Objects.requireNonNull(scopeKey, "scopeKey");
        DSLContext dsl = ensureDSL();

        Record1<Long> r = dsl
            .select(VN_INODE.ID)
            .from(VN_INODE)
            .where(VN_INODE.SCOPE_KEY.eq(Ltree.ltree(scopeKey)))
            .fetchOne();

        return r == null ? Optional.empty() : Optional.ofNullable(r.value1());
    }

    /**
     * Resolve the nearest existing inode for the given scope key.
     *
     * Semantics:
     * - If there is an exact match, returns that inode id.
     * - Otherwise, returns the deepest (longest) existing ancestor scope key.
     * - Returns empty if no ancestor exists.
     *
     * This is the natural place to leverage ltree containment operators.
     */
    public Optional<Long> findNearestExistingInodeIdByScopeKey(String scopeKeyString) {
        if (scopeKeyString == null || scopeKeyString.isBlank()) {
            return Optional.empty();
        }

        DSLContext dsl = ensureDSL();
		
		Param<Ltree> target = DSL.val(Ltree.ltree(scopeKeyString), VN_INODE.SCOPE_KEY.getDataType() );

        // candidate @> target  ==> candidate is ancestor of target (or equals)
        // pick the deepest ancestor by ordering on nlevel(scope_key) DESC
         
		Field<Integer> nlevel = DSL.field("nlevel({0})", Integer.class, VN_INODE.SCOPE_KEY);
		
        return dsl
            .select(VN_INODE.ID)
            .from(VN_INODE)
            .where(condition("{0} @> {1}", VN_INODE.SCOPE_KEY, target))
            .orderBy(nlevel.desc())
            .limit(1)
            .fetchOptional(VN_INODE.ID);
    }

    public Optional<Inode> findById(Long id) {
        Objects.requireNonNull(id, "id");

        DSLContext dsl = ensureDSL();
        
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
        
        DSLContext dsl = ensureDSL();
        
        Ltree scope = entity.scopeKey() == null ? null :   Ltree.ltree(entity.scopeKey());
        VnInodeRecord inserted = dsl
            .insertInto(VN_INODE)
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

    

    
    boolean pathExists(String path) {
    	Objects.requireNonNull(path, "path");	
    DSLContext dsl = ensureDSL();
  
    String sk = PathKeyCodec.toLTree(path);
	Param<Ltree> scopeKey = DSL.val(Ltree.ltree(sk), VN_INODE.SCOPE_KEY.getDataType());
    		
    return dsl.fetchExists(
        dsl.selectOne()
           .from(VN_INODE)
           .where(VN_INODE.SCOPE_KEY.eq(scopeKey)
           )
    );
    }

    public List<InodePathSegment> findAllByInodeIdOrderByOrdAsc(Long inodeId) {
        Objects.requireNonNull(inodeId, "inodeId");

        DSLContext dsl = ensureDSL();
        
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

        DSLContext dsl = ensureDSL();
        
        List<InodePathSegment> list = new ArrayList<>();
        for (InodePathSegment e : entities) {
            Objects.requireNonNull(e, "entity");
            Objects.requireNonNull(e.inode(), "entity.inode");
            Objects.requireNonNull(e.inode().id(), "entity.inode.id");
            Objects.requireNonNull(e.dirEntry(), "entity.dirEntry");
            Objects.requireNonNull(e.dirEntry().id(), "entity.dirEntry.id");

            dsl.insertInto(VN_INODE_PATH_SEGMENT)
                .set(VN_INODE_PATH_SEGMENT.INODE_ID, e.inode().id())
                .set(VN_INODE_PATH_SEGMENT.ORD, e.ord())
                .set(VN_INODE_PATH_SEGMENT.DIR_ENTRY_ID, e.dirEntry().id())
                .execute();

            list.add(e);
        }

        return list;
    }

    public Optional<DirEntry> findByParentIdAndName(Long parentInodeId, String name) {
        Objects.requireNonNull(parentInodeId, "parentInodeId");
        Objects.requireNonNull(name, "name");

        DSLContext dsl = ensureDSL();
        
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

        DSLContext dsl = ensureDSL();
        
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

        DSLContext dsl = ensureDSL();
        
        var inserted = dsl
            .insertInto(VN_DIR_ENTRY)
            .set(VN_DIR_ENTRY.PARENT_ID, dir.parent().id())
            .set(VN_DIR_ENTRY.NAME, dir.name())
            .set(VN_DIR_ENTRY.CHILD_ID, dir.child().id())
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
        
    	
        String pScope = r.get(P.SCOPE_KEY) == null ? null : r.get(P.SCOPE_KEY).toString();
        String cScope = r.get(C.SCOPE_KEY) == null ? null : r.get(C.SCOPE_KEY).toString();

        Inode parent = new Inode(r.get(P.ID), r.get(P.CREATED_AT).toInstant(), pScope);
        Inode child  = new Inode(r.get(C.ID), r.get(C.CREATED_AT).toInstant(), cScope);

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

        return new InodePathSegment(
            r.get(VN_INODE_PATH_SEGMENT.ID),
            inode,
            r.get(VN_INODE_PATH_SEGMENT.ORD),
            dirEntry,
            r.get(VN_INODE_PATH_SEGMENT.CREATED_AT).toInstant()
        );
    }
    
    public Inode rootInode() {
  	  Long rootId = findIdByScopeKey(PathKeyCodec.ROOT_LABEL)
  	      .orElseThrow(() -> new IllegalStateException("Root inode missing for scope_key=" + PathKeyCodec.ROOT_LABEL));
  	  return findById(rootId)
  	      .orElseThrow(() -> new IllegalStateException("Root inode missing id=" + rootId));
  	}

    public void ensureRootInode() {
       
        DSLContext dsl = ensureDSL();

        boolean exists = dsl.fetchExists(
            dsl.selectOne()
               .from(VN_INODE)
               .where(VN_INODE.SCOPE_KEY.eq(Ltree.ltree(PathKeyCodec.ROOT_LABEL)))
        );

        if (!exists) {
            dsl.insertInto(VN_INODE)
               .set(VN_INODE.SCOPE_KEY, Ltree.ltree(PathKeyCodec.ROOT_LABEL))
               .execute();
        }
    }
    
    
    public Inode resolveOrCreateInode(String nodePath) {
    Objects.requireNonNull(nodePath, "nodePath");

    // Normalize for consistent semantics (callers sometimes pass trailing slash, etc.)
    nodePath = PathUtils.normalizePath(nodePath);

    // Ensure the root exists (scope_key = ROOT_LABEL)
    ensureRootInode();

    List<String> segs = PathUtils.splitSegments(nodePath);
    Inode root = rootInode();

    // Fast path: root
    if (segs.isEmpty()) {
        return root;
    }

    // Compute the full scope key for the target path, and keep prefix scope keys so we can
    // map the nearest existing inode back to a segment index.
    String curScope = root.scopeKey();
    List<String> prefixScopeKeys = new ArrayList<>(segs.size());
    for (String seg : segs) {
        curScope = PathKeyCodec.appendSegLabel(curScope, PathKeyCodec.label(seg));
        prefixScopeKeys.add(curScope);
    }
    String targetScopeKey = prefixScopeKeys.get(prefixScopeKeys.size() - 1);

    // Find the deepest existing ancestor (or self) by scope_key.
    // If none is found, fall back to root (should not happen if root exists).
    Inode cur = root;
    List<InodePathSegment> curSegs = List.of();

    Optional<Long> nearestIdOpt = findNearestExistingInodeIdByScopeKey(targetScopeKey);
    if (nearestIdOpt.isPresent()) {
        Inode nearest = findById(nearestIdOpt.get()).orElse(root);
        cur = nearest;

        // Determine how many segments are already present in this nearest inode.
        // This lets us avoid re-walking/reading the graph for the prefix we already have.
        int existingDepth = 0;
        String nearestScope = nearest.scopeKey();

        if (nearestScope != null && nearestScope.equals(root.scopeKey())) {
            existingDepth = 0;
        } else {
            int idx = prefixScopeKeys.indexOf(nearestScope);
            existingDepth = (idx >= 0) ? (idx + 1) : 0;
        }

        // Materialized path segments for the current inode; reused/extended as we create new nodes.
        curSegs = (cur.id() != null) ? findAllByInodeIdOrderByOrdAsc(cur.id()) : List.of();

        // Create remaining suffix starting from the nearest existing depth.
        for (int i = existingDepth; i < segs.size(); i++) {
            String seg = segs.get(i);

            // If the child edge already exists under this inode, follow it.
            Optional<DirEntry> e = findByParentIdAndName(cur.id(), seg);
            if (e.isPresent()) {
                cur = e.get().child();
                curSegs = findAllByInodeIdOrderByOrdAsc(cur.id());
                continue;
            }

            String childScopeKey = prefixScopeKeys.get(i);
            Inode child = save(new Inode(childScopeKey));

            DirEntry entry = save(new DirEntry(cur, seg, child));

            // Build the child's materialized path segments by copying the parent's segments
            // and appending the new edge at the next ordinal.
            List<InodePathSegment> childSegs = new ArrayList<>(curSegs.size() + 1);
            for (InodePathSegment ps : curSegs) {
                childSegs.add(new InodePathSegment(child, ps.ord(), ps.dirEntry()));
            }
            childSegs.add(new InodePathSegment(child, curSegs.size(), entry));
            saveAll(childSegs);

            cur = child;
            curSegs = childSegs;
        }

        return cur;
    }

    // Conservative fallback: old behavior (walk from root via dir entries).
    // This should be rare if scope_key is kept coherent.
    LOGGER.error("Scope key is not coherent");
    cur = root;
    for (String seg : segs) {
        Optional<DirEntry> e = findByParentIdAndName(cur.id(), seg);
        if (e.isPresent()) {
            cur = e.get().child();
            continue;
        }

        String childScopeKey = PathKeyCodec.appendSegLabel(cur.scopeKey(), PathKeyCodec.label(seg));
        Inode child = save(new Inode(childScopeKey));
        DirEntry entry = save(new DirEntry(cur, seg, child));

        List<InodePathSegment> parentSegs = findAllByInodeIdOrderByOrdAsc(cur.id());
        List<InodePathSegment> childSegs = new ArrayList<>(parentSegs.size() + 1);
        for (InodePathSegment ps : parentSegs) {
            childSegs.add(new InodePathSegment(child, ps.ord(), ps.dirEntry()));
        }
        childSegs.add(new InodePathSegment(child, parentSegs.size(), entry));
        saveAll(childSegs);

        cur = child;
    }
    return cur;
}
 
  
    
   private static Inode toInode(VnInodeRecord r) {
        
        String scopeKey = r.getScopeKey() == null ? null : r.getScopeKey().toString();
        return new Inode(r.getId(), r.getCreatedAt().toInstant(), scopeKey);
    }
}
