package com.veritynow.core.store.versionstore.repo;

import static com.veritynow.core.store.persistence.jooq.Tables.VN_DIR_ENTRY;
import static com.veritynow.core.store.persistence.jooq.Tables.VN_INODE;
import static com.veritynow.core.store.persistence.jooq.Tables.VN_INODE_PATH_SEGMENT;
import static com.veritynow.core.store.persistence.jooq.Tables.VN_NODE_HEAD;
import static com.veritynow.core.store.persistence.jooq.Tables.VN_NODE_VERSION;
import static org.jooq.impl.DSL.condition;
import static org.jooq.impl.DSL.currentOffsetDateTime;
import static org.jooq.impl.DSL.excluded;
import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.name;
import static org.jooq.impl.DSL.select;
import static org.jooq.impl.DSL.table;

import java.sql.Connection;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.jooq.CommonTableExpression;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.InsertSetMoreStep;
import org.jooq.Record;
import org.jooq.Record2;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.jooq.postgres.extensions.types.Ltree;

import com.veritynow.core.context.Context;
import com.veritynow.core.store.meta.VersionMeta;
import com.veritynow.core.store.persistence.jooq.tables.records.VnNodeVersionRecord;
import com.veritynow.core.store.txn.TransactionContext;

public final class VersionMetaRepository {

	private final DSLContext defaultDSL;
	

	public VersionMetaRepository(DSLContext dsl) {
		this.defaultDSL = Objects.requireNonNull(dsl, "dsl");
		
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
		// txn-bound DSL (no singleton leakage)
		return DSL.using(conn, SQLDialect.POSTGRES);
	}

	// -------------------------
	// Existing API
	// -------------------------

	public List<VersionMeta> findAllByInodeIdOrderByTimestampDescIdDesc(Long inodeId) {

		Objects.requireNonNull(inodeId, "inodeId");
		DSLContext dsl = ensureDSL();

		// Single inode: materialize path once, then map rows (avoid N+1).
		final String path = materializePath(inodeId);

		List<VnNodeVersionRecord> rows = dsl.selectFrom(VN_NODE_VERSION)
				.where(VN_NODE_VERSION.INODE_ID.eq(inodeId))
				.orderBy(VN_NODE_VERSION.TIMESTAMP.desc(), VN_NODE_VERSION.ID.desc())
				.fetchInto(VnNodeVersionRecord.class);

		List<VersionMeta> out = new ArrayList<>(rows.size());
		for (VnNodeVersionRecord r : rows) {
			out.add(nodeVersionToVersionMeta(r, path));
		}
		return out;
	}

	public VersionMeta save(VersionMeta vm, Long inodeId) {

		Objects.requireNonNull(vm, "vme");
		Objects.requireNonNull(inodeId, "inodeId");

		DSLContext dsl = ensureDSL();

		VnNodeVersionRecord inserted = dsl.insertInto(VN_NODE_VERSION)
				.set(VN_NODE_VERSION.INODE_ID, inodeId)
				.set(VN_NODE_VERSION.OPERATION, vm.operation())
				.set(VN_NODE_VERSION.PRINCIPAL, vm.principal())
				.set(VN_NODE_VERSION.CORRELATION_ID, vm.correlationId())
				.set(VN_NODE_VERSION.WORKFLOW_ID, vm.workflowId())
				.set(VN_NODE_VERSION.CONTEXT_NAME, vm.contextName())
				.set(VN_NODE_VERSION.TRANSACTION_ID, vm.transactionId())
				.set(VN_NODE_VERSION.TRANSACTION_RESULT, vm.transactionResult())
				.set(VN_NODE_VERSION.HASH_ALGORITHM, vm.hashAlgorithm())
				.set(VN_NODE_VERSION.HASH, vm.hash())
				.set(VN_NODE_VERSION.NAME, vm.name())
				.set(VN_NODE_VERSION.MIME_TYPE, vm.mimeType())
				.set(VN_NODE_VERSION.SIZE, vm.size())
				.returning(VN_NODE_VERSION.fields())
				.fetchOneInto(VnNodeVersionRecord.class);

		if (inserted == null || inserted.getId() == null) {
			throw new IllegalStateException("Insert into vn_node_version did not return an id");
		}

		final String path = materializePath(inodeId);
		return nodeVersionToVersionMeta(inserted, path);
	}

	public Optional<VersionMeta> findLatestVersionByInodeId(Long inodeId) {
		Objects.requireNonNull(inodeId, "inodeId");

		DSLContext dsl = ensureDSL();

		return dsl.select(VN_NODE_VERSION.fields()).from(VN_NODE_HEAD).join(VN_NODE_VERSION)
				.on(VN_NODE_VERSION.ID.eq(VN_NODE_HEAD.VERSION_ID)).where(VN_NODE_HEAD.INODE_ID.eq(inodeId))
				.fetchOptional(this::fromHeadVersiontoVersionMeta);
	}

	public List<VersionMeta> findByTransactionId(String transactionId) {
		Objects.requireNonNull(transactionId, "transactionId");

		DSLContext dsl = ensureDSL();

		List<VnNodeVersionRecord> rows = dsl.selectFrom(VN_NODE_VERSION)
				.where(VN_NODE_VERSION.TRANSACTION_ID.eq(transactionId))
				.orderBy(VN_NODE_VERSION.TIMESTAMP.desc(), VN_NODE_VERSION.ID.desc())
				.fetchInto(VnNodeVersionRecord.class);

		return mapNodeVersionRecords(rows);
	}

	public List<VersionMeta> findByCorrelationId(String correlationId) {
		Objects.requireNonNull(correlationId, "correlationId");

		DSLContext dsl = ensureDSL();

		List<VnNodeVersionRecord> rows = dsl.selectFrom(VN_NODE_VERSION)
				.where(VN_NODE_VERSION.CORRELATION_ID.eq(correlationId))
				.orderBy(VN_NODE_VERSION.TIMESTAMP.desc(), VN_NODE_VERSION.ID.desc())
				.fetchInto(VnNodeVersionRecord.class);

		return mapNodeVersionRecords(rows);
	}

	public List<VersionMeta> findByCorrelationIdAndTransactionId(String correlationId, String transactionId) {
		Objects.requireNonNull(correlationId, "correlationId");
		Objects.requireNonNull(transactionId, "transactionId");

		DSLContext dsl = ensureDSL();

		List<VnNodeVersionRecord> rows = dsl.selectFrom(VN_NODE_VERSION)
				.where(VN_NODE_VERSION.CORRELATION_ID.eq(correlationId).and(VN_NODE_VERSION.TRANSACTION_ID.eq(transactionId)))
				.orderBy(VN_NODE_VERSION.TIMESTAMP.desc(), VN_NODE_VERSION.ID.desc())
				.fetchInto(VnNodeVersionRecord.class);

		return mapNodeVersionRecords(rows);
	}

	public List<VersionMeta> findByWorkflowId(String workflowId) {
		Objects.requireNonNull(workflowId, "workflowId");

		DSLContext dsl = ensureDSL();

		List<VnNodeVersionRecord> rows = dsl.selectFrom(VN_NODE_VERSION)
				.where(VN_NODE_VERSION.WORKFLOW_ID.eq(workflowId))
				.orderBy(VN_NODE_VERSION.TIMESTAMP.desc(), VN_NODE_VERSION.ID.desc())
				.fetchInto(VnNodeVersionRecord.class);

		return mapNodeVersionRecords(rows);
	}

	public List<VersionMeta> findByWorkflowIdAndCorrelationId(String workflowId, String correlationId) {
		Objects.requireNonNull(workflowId, "workflowId");
		Objects.requireNonNull(correlationId, "correlationId");

		DSLContext dsl = ensureDSL();

		List<VnNodeVersionRecord> rows = dsl.selectFrom(VN_NODE_VERSION)
				.where(VN_NODE_VERSION.WORKFLOW_ID.eq(workflowId).and(VN_NODE_VERSION.CORRELATION_ID.eq(correlationId)))
				.orderBy(VN_NODE_VERSION.TIMESTAMP.desc(), VN_NODE_VERSION.ID.desc())
				.fetchInto(VnNodeVersionRecord.class);

		return mapNodeVersionRecords(rows);
	}

	public List<VersionMeta> findByWorkflowIdAndCorrelationIdAndTransactionId(String workflowId, String correlationId,
			String transactionId) {
		Objects.requireNonNull(workflowId, "workflowId");
		Objects.requireNonNull(correlationId, "correlationId");
		Objects.requireNonNull(transactionId, "transactionId");

		DSLContext dsl = ensureDSL();

		List<VnNodeVersionRecord> rows = dsl.selectFrom(VN_NODE_VERSION)
				.where(VN_NODE_VERSION.CORRELATION_ID.eq(correlationId).and(VN_NODE_VERSION.WORKFLOW_ID.eq(workflowId)).and(VN_NODE_VERSION.TRANSACTION_ID.eq(transactionId)))
				.orderBy(VN_NODE_VERSION.TIMESTAMP.desc(), VN_NODE_VERSION.ID.desc())
				.fetchInto(VnNodeVersionRecord.class);

		return mapNodeVersionRecords(rows);
	}

	public int saveAndPublish(VersionMeta vm, Long inodeId) {
		Objects.requireNonNull(vm, "vm");
		Objects.requireNonNull(inodeId, "inodeId");

		CommonTableExpression<Record2<Long, Long>> ins = name("ins").fields("version_id", "inode_id")
				.as(insertVersionMeta(vm, inodeId).returningResult(VN_NODE_VERSION.ID, VN_NODE_VERSION.INODE_ID));

		Field<Long> vId = field(name("ins", "version_id"), Long.class);
		Field<Long> iId = field(name("ins", "inode_id"), Long.class);
		Field<OffsetDateTime> now = currentOffsetDateTime();

		DSLContext dsl = ensureDSL();

		return dsl.with(ins)
				.insertInto(VN_NODE_HEAD, VN_NODE_HEAD.INODE_ID, VN_NODE_HEAD.VERSION_ID, VN_NODE_HEAD.UPDATED_AT)
				.select(select(iId, vId, now).from(table(name("ins")))).onConflict(VN_NODE_HEAD.INODE_ID).doUpdate()
				.set(VN_NODE_HEAD.VERSION_ID, excluded(VN_NODE_HEAD.VERSION_ID)).set(VN_NODE_HEAD.UPDATED_AT, now)
				.execute();
	}

	List<VersionMeta> getWorkflows(Long inodeId) {

		DSLContext dsl = ensureDSL();

		List<VnNodeVersionRecord> rows = dsl.selectFrom(VN_NODE_VERSION)
				.where(VN_NODE_VERSION.WORKFLOW_ID.in(
					dsl.select(VN_NODE_VERSION.WORKFLOW_ID)
						.from(VN_NODE_VERSION)
						.where(VN_NODE_VERSION.INODE_ID.eq(inodeId))
						.and(VN_NODE_VERSION.WORKFLOW_ID.isNotNull())
				))
				.orderBy(VN_NODE_VERSION.WORKFLOW_ID.asc(), VN_NODE_VERSION.TIMESTAMP.desc(), VN_NODE_VERSION.ID.desc())
				.fetchInto(VnNodeVersionRecord.class);

		return mapNodeVersionRecords(rows);
	}

	List<VersionMeta> getCorrelations(Long inodeId) {

		DSLContext dsl = ensureDSL();

		List<VnNodeVersionRecord> rows = dsl.selectFrom(VN_NODE_VERSION)
				.where(VN_NODE_VERSION.CORRELATION_ID.in(
					dsl.select(VN_NODE_VERSION.CORRELATION_ID)
						.from(VN_NODE_VERSION)
						.where(VN_NODE_VERSION.INODE_ID.eq(inodeId))
						.and(VN_NODE_VERSION.WORKFLOW_ID.isNotNull())
				))
				.orderBy(VN_NODE_VERSION.WORKFLOW_ID.asc(), VN_NODE_VERSION.TIMESTAMP.desc(), VN_NODE_VERSION.ID.desc())
				.fetchInto(VnNodeVersionRecord.class);

		return mapNodeVersionRecords(rows);
	}

	List<VersionMeta> getTransactions(Long inodeId) {

		DSLContext dsl = ensureDSL();

		List<VnNodeVersionRecord> rows = dsl.selectFrom(VN_NODE_VERSION)
				.where(VN_NODE_VERSION.TRANSACTION_ID.in(
					dsl.select(VN_NODE_VERSION.TRANSACTION_ID)
						.from(VN_NODE_VERSION)
						.where(VN_NODE_VERSION.INODE_ID.eq(inodeId))
						.and(VN_NODE_VERSION.WORKFLOW_ID.isNotNull())
				))
				.orderBy(VN_NODE_VERSION.WORKFLOW_ID.asc(), VN_NODE_VERSION.TIMESTAMP.desc(), VN_NODE_VERSION.ID.desc())
				.fetchInto(VnNodeVersionRecord.class);

		return mapNodeVersionRecords(rows);
	}

	// -------------------------
	// New: bulk head fetch (fix N+1)
	// -------------------------

	public List<VersionMeta> findLatestVersionsByInodeIds(List<Long> inodeIds) {
		Objects.requireNonNull(inodeIds, "inodeIds");

		if (inodeIds.isEmpty()) return List.of();
		DSLContext dsl = ensureDSL();

		List<Record> rows = dsl.select(VN_NODE_VERSION.fields())
				.from(VN_NODE_HEAD)
				.join(VN_NODE_VERSION).on(VN_NODE_VERSION.ID.eq(VN_NODE_HEAD.VERSION_ID))
				.where(VN_NODE_HEAD.INODE_ID.in(inodeIds))
				.fetch();

		return mapHeadRecords(rows);
	}

	// -------------------------
	// New: ltree-powered traversal via scope_key
	// -------------------------

	/**
	 * Latest versions for every inode in the subtree rooted at
	 * {@code rootScopeKey}, including the root itself.
	 *
	 * Uses: vn_inode.scope_key <@ rootScopeKey
	 */
	public List<VersionMeta> findLatestVersionsInSubtree(String rootScopeKey) {
		Objects.requireNonNull(rootScopeKey, "rootScopeKey");

		DSLContext dsl = ensureDSL();
		Ltree root = Ltree.ltree(rootScopeKey);

		Condition inSubtree = condition("{0} <@ {1}", VN_INODE.SCOPE_KEY, root);

		List<Record> rows = dsl.select(VN_NODE_VERSION.fields())
				.from(VN_INODE)
				.join(VN_NODE_HEAD).on(VN_NODE_HEAD.INODE_ID.eq(VN_INODE.ID))
				.join(VN_NODE_VERSION).on(VN_NODE_VERSION.ID.eq(VN_NODE_HEAD.VERSION_ID))
				.where(inSubtree)
				.fetch();

		return mapHeadRecords(rows);
	}

	/**
	 * Latest versions for the *immediate children* of {@code parentScopeKey}.
	 *
	 * Uses: - scope_key <@ parent - nlevel(scope_key) = nlevel(parent) + 1
	 */
	public List<VersionMeta> findLatestVersionsOfChildren(String parentScopeKey) {
		Objects.requireNonNull(parentScopeKey, "parentScopeKey");

		DSLContext dsl = ensureDSL();
		Ltree parent = Ltree.ltree(parentScopeKey);

		Condition inSubtree = condition("{0} <@ {1}", VN_INODE.SCOPE_KEY, parent);
		Condition depthIsChild = condition("nlevel({0}) = nlevel({1}) + 1", VN_INODE.SCOPE_KEY, parent);

		List<Record> rows = dsl.select(VN_NODE_VERSION.fields())
				.from(VN_INODE)
				.join(VN_NODE_HEAD).on(VN_NODE_HEAD.INODE_ID.eq(VN_INODE.ID))
				.join(VN_NODE_VERSION).on(VN_NODE_VERSION.ID.eq(VN_NODE_HEAD.VERSION_ID))
				.where(inSubtree.and(depthIsChild))
				.orderBy(VN_INODE.SCOPE_KEY.asc())
				.fetch();

		return mapHeadRecords(rows);
	}

	// -------------------------
	// Internals
	// -------------------------

	/**
     * Materialize the (single, canonical) string path for one or more inodes.
     *
     * IMPORTANT: This assumes a single-path model (no hardlinks / multiple parents).
     *
     * Derivation strategy:
     *  - vn_inode_path_segment provides the ordered list of segments (ORD ASC)
     *  - vn_dir_entry.name is the human-readable segment for each step
     *
     * For the root inode (no segments), returns "/".
     *
     * PERF: For queries that return many versions (or many inodes), always use the
     * batch form to avoid N+1 path reconstruction queries.
     */
    @SuppressWarnings("unused")
	private Map<Long, String> materializePaths(Collection<Long> inodeIds) {
        Objects.requireNonNull(inodeIds, "inodeIds");
        if (inodeIds.isEmpty()) return Map.of();

        DSLContext dsl = ensureDSL();

        List<Record2<Long, String>> rows = dsl
            .select(VN_INODE_PATH_SEGMENT.INODE_ID, VN_DIR_ENTRY.NAME)
            .from(VN_INODE_PATH_SEGMENT)
            .join(VN_DIR_ENTRY).on(VN_DIR_ENTRY.ID.eq(VN_INODE_PATH_SEGMENT.DIR_ENTRY_ID))
            .where(VN_INODE_PATH_SEGMENT.INODE_ID.in(inodeIds))
            .orderBy(VN_INODE_PATH_SEGMENT.INODE_ID.asc(), VN_INODE_PATH_SEGMENT.ORD.asc())
            .fetch();

        Map<Long, List<String>> segsByInode = new HashMap<>();
        for (Record2<Long, String> row : rows) {
            Long inodeId = row.value1();
            String name = row.value2();
            segsByInode.computeIfAbsent(inodeId, _k -> new ArrayList<>()).add(name);
        }

        Map<Long, String> out = new HashMap<>();
        for (Long inodeId : inodeIds) {
            List<String> segs = segsByInode.get(inodeId);
            if (segs == null || segs.isEmpty()) {
                out.put(inodeId, "/");
            } else {
                out.put(inodeId, "/" + String.join("/", segs));
            }
        }
        return out;
    }

    private String materializePath(Long inodeId) {
        Objects.requireNonNull(inodeId, "inodeId");
        return materializePaths(List.of(inodeId)).get(inodeId);
    }

	
	
	private InsertSetMoreStep<VnNodeVersionRecord> insertVersionMeta(VersionMeta vm, Long inodeId) {
		DSLContext dsl = ensureDSL();

		return dsl.insertInto(VN_NODE_VERSION).set(VN_NODE_VERSION.INODE_ID, inodeId)
				.set(VN_NODE_VERSION.OPERATION, vm.operation()).set(VN_NODE_VERSION.PRINCIPAL, vm.principal())
				.set(VN_NODE_VERSION.CORRELATION_ID, vm.correlationId())
				.set(VN_NODE_VERSION.WORKFLOW_ID, vm.workflowId()).set(VN_NODE_VERSION.CONTEXT_NAME, vm.contextName())
				.set(VN_NODE_VERSION.TRANSACTION_ID, vm.transactionId())
				.set(VN_NODE_VERSION.TRANSACTION_RESULT, vm.transactionResult())
				.set(VN_NODE_VERSION.HASH_ALGORITHM, vm.hashAlgorithm()).set(VN_NODE_VERSION.HASH, vm.hash())
				.set(VN_NODE_VERSION.NAME, vm.name()).set(VN_NODE_VERSION.MIME_TYPE, vm.mimeType())
				.set(VN_NODE_VERSION.SIZE, vm.size());
	}

	
	
	
	
	private List<VersionMeta> mapNodeVersionRecords(List<VnNodeVersionRecord> rows) {
		if (rows == null || rows.isEmpty()) return List.of();

		Set<Long> inodeIds = new HashSet<>();
		for (VnNodeVersionRecord r : rows) {
			Long inodeId = r.getInodeId();
			if (inodeId != null) inodeIds.add(inodeId);
		}
		Map<Long, String> paths = materializePaths(inodeIds);

		List<VersionMeta> out = new ArrayList<>(rows.size());
		for (VnNodeVersionRecord r : rows) {
			Long inodeId = r.getInodeId();
			out.add(nodeVersionToVersionMeta(r, inodeId == null ? null : paths.get(inodeId)));
		}
		return out;
	}

	private List<VersionMeta> mapHeadRecords(List<Record> rows) {
		if (rows == null || rows.isEmpty()) return List.of();

		Set<Long> inodeIds = new HashSet<>();
		for (Record r : rows) {
			Long inodeId = r.get(VN_NODE_VERSION.INODE_ID);
			if (inodeId != null) inodeIds.add(inodeId);
		}
		Map<Long, String> paths = materializePaths(inodeIds);

		List<VersionMeta> out = new ArrayList<>(rows.size());
		for (Record r : rows) {
			Long inodeId = r.get(VN_NODE_VERSION.INODE_ID);
			out.add(fromHeadVersiontoVersionMeta(r, inodeId == null ? null : paths.get(inodeId)));
		}
		return out;
	}

	private VersionMeta fromHeadVersiontoVersionMeta(Record r, String path) {
		Long size = r.get(VN_NODE_VERSION.SIZE);
		return new VersionMeta(
			r.get(VN_NODE_VERSION.HASH_ALGORITHM),
			r.get(VN_NODE_VERSION.HASH),
			r.get(VN_NODE_VERSION.NAME),
			r.get(VN_NODE_VERSION.MIME_TYPE),
			size == null ? 0L : size,
			path,
			r.get(VN_NODE_VERSION.TIMESTAMP),
			r.get(VN_NODE_VERSION.OPERATION),
			r.get(VN_NODE_VERSION.PRINCIPAL),
			r.get(VN_NODE_VERSION.CORRELATION_ID),
			r.get(VN_NODE_VERSION.WORKFLOW_ID),
			r.get(VN_NODE_VERSION.CONTEXT_NAME),
			r.get(VN_NODE_VERSION.TRANSACTION_ID),
			r.get(VN_NODE_VERSION.TRANSACTION_RESULT)
		);
	}

	private VersionMeta fromHeadVersiontoVersionMeta(Record r) {
		Long inodeId = r.get(VN_NODE_VERSION.INODE_ID);
		String path = inodeId == null ? null : materializePath(inodeId);
		return fromHeadVersiontoVersionMeta(r, path);
	}

	private VersionMeta nodeVersionToVersionMeta(VnNodeVersionRecord r, String path) {
		Long size = r.getSize();
		return new VersionMeta(
			r.getHashAlgorithm(),
			r.getHash(),
			r.getName(),
			r.getMimeType(),
			size == null ? 0L : size,
			path,
			r.getTimestamp(),
			r.getOperation(),
			r.getPrincipal(),
			r.getCorrelationId(),
			r.getWorkflowId(),
			r.getContextName(),
			r.getTransactionId(),
			r.getTransactionResult()
		);
	}


}

