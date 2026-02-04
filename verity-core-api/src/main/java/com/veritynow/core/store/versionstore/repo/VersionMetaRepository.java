package com.veritynow.core.store.versionstore.repo;

import static com.veritynow.core.store.persistence.jooq.Tables.VN_NODE_HEAD;
import static com.veritynow.core.store.persistence.jooq.Tables.VN_NODE_VERSION;
import static org.jooq.impl.DSL.currentOffsetDateTime;
import static org.jooq.impl.DSL.excluded;
import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.name;
import static org.jooq.impl.DSL.select;
import static org.jooq.impl.DSL.table;

import java.sql.Connection;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.jooq.CommonTableExpression;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.InsertSetMoreStep;
import org.jooq.Record;
import org.jooq.Record2;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

import com.veritynow.core.context.Context;
import com.veritynow.core.store.meta.VersionMeta;
import com.veritynow.core.store.persistence.jooq.tables.records.VnNodeVersionRecord;
import com.veritynow.core.store.txn.TransactionContext;

public final class VersionMetaRepository {

	private final DSLContext defaultDSL;
	private  DSLContext txnDSL = null;

	public VersionMetaRepository(DSLContext dsl) {
		this.defaultDSL = Objects.requireNonNull(dsl, "dsl");
	}

	private DSLContext ensureDSL() {
		if (!Context.isActive()) {
			txnDSL = null;
			return defaultDSL;
		}
		String txnId = Context.transactionIdOrNull();
		if (txnId == null) {
			txnDSL = null;
			return defaultDSL;
		}
		Connection conn = TransactionContext.getConnection(txnId);
		if (conn == null) {
			txnDSL = null;
			return defaultDSL;
		}

		 
    	if (txnDSL == null) {
    		txnDSL = DSL.using(conn, SQLDialect.POSTGRES);
    	}
    	
		return txnDSL;

	}

	public List<VersionMeta> findAllByInodeIdOrderByTimestampDescIdDesc(Long inodeId) {
		Objects.requireNonNull(inodeId, "inodeId");

		DSLContext dsl = ensureDSL();

		return dsl.selectFrom(VN_NODE_VERSION).where(VN_NODE_VERSION.INODE_ID.eq(inodeId))
				.orderBy(VN_NODE_VERSION.TIMESTAMP.desc(), VN_NODE_VERSION.ID.desc())
				.fetch(this::nodeVersionToVersionMeta);
	}

	public VersionMeta save(VersionMeta vm, Long inodeId) {
		Objects.requireNonNull(vm, "vme");
		Objects.requireNonNull(inodeId, "inodeId");

		DSLContext dsl = ensureDSL();

		VnNodeVersionRecord inserted = dsl.insertInto(VN_NODE_VERSION).set(VN_NODE_VERSION.INODE_ID, inodeId)
				// .set(VN_NODE_VERSION.TIMESTAMP, DBTime.nowEpochMs()) set by DB
				.set(VN_NODE_VERSION.PATH, vm.path()).set(VN_NODE_VERSION.OPERATION, vm.operation())
				.set(VN_NODE_VERSION.PRINCIPAL, vm.principal()).set(VN_NODE_VERSION.CORRELATION_ID, vm.correlationId())
				.set(VN_NODE_VERSION.WORKFLOW_ID, vm.workflowId()).set(VN_NODE_VERSION.CONTEXT_NAME, vm.contextName())
				.set(VN_NODE_VERSION.TRANSACTION_ID, vm.transactionId())
				.set(VN_NODE_VERSION.TRANSACTION_RESULT, vm.transactionResult())
				.set(VN_NODE_VERSION.HASH_ALGORITHM, vm.hashAlgorithm()).set(VN_NODE_VERSION.HASH, vm.hash())
				.set(VN_NODE_VERSION.NAME, vm.name()).set(VN_NODE_VERSION.MIME_TYPE, vm.mimeType())
				.set(VN_NODE_VERSION.SIZE, vm.size()).returning(VN_NODE_VERSION.fields()) // <-- key change
				.fetchOneInto(VnNodeVersionRecord.class);

		if (inserted == null || inserted.getId() == null) {
			throw new IllegalStateException("Insert into vn_node_version did not return an id");
		}

		return nodeVersionToVersionMeta(inserted);

	}

	public Optional<VersionMeta> findLatestVersionByInodeId(Long inodeId) {
		Objects.requireNonNull(inodeId, "inodeId");

		DSLContext dsl = ensureDSL();

		return dsl.select(VN_NODE_VERSION.fields()).from(VN_NODE_HEAD).join(VN_NODE_VERSION)
				.on(VN_NODE_VERSION.ID.eq(VN_NODE_HEAD.VERSION_ID)).where(VN_NODE_HEAD.INODE_ID.eq(inodeId))
				.fetchOptional(this::fromHeadVersiontoVersionMeta); // mapper reads VN_NODE_VERSION only
	}

	public List<VersionMeta> findByTransactionId(String transactionId) {
		Objects.requireNonNull(transactionId, "transactionId");

		DSLContext dsl = ensureDSL();

		return dsl.selectFrom(VN_NODE_VERSION).where(VN_NODE_VERSION.TRANSACTION_ID.eq(transactionId))
				.orderBy(VN_NODE_VERSION.TIMESTAMP.desc(), VN_NODE_VERSION.ID.desc())
				.fetch(this::nodeVersionToVersionMeta);
	}

	public List<VersionMeta> findByCorrelationId(String correlationId) {
		Objects.requireNonNull(correlationId, "correlationId");

		DSLContext dsl = ensureDSL();

		return dsl.selectFrom(VN_NODE_VERSION).where(VN_NODE_VERSION.CORRELATION_ID.eq(correlationId))
				.orderBy(VN_NODE_VERSION.TIMESTAMP.desc(), VN_NODE_VERSION.ID.desc())
				.fetch(this::nodeVersionToVersionMeta);
	}

	public List<VersionMeta> findByCorrelationIdAndTransactionId(String correlationId, String transactionId) {
		Objects.requireNonNull(correlationId, "correlationId");
		Objects.requireNonNull(transactionId, "transactionId");

		DSLContext dsl = ensureDSL();

		return dsl.selectFrom(VN_NODE_VERSION)
				.where(VN_NODE_VERSION.CORRELATION_ID.eq(correlationId)
						.and(VN_NODE_VERSION.TRANSACTION_ID.eq(transactionId)))
				.orderBy(VN_NODE_VERSION.TIMESTAMP.desc(), VN_NODE_VERSION.ID.desc())
				.fetch(this::nodeVersionToVersionMeta);
	}

	public List<VersionMeta> findByWorkflowId(String workflowId) {
		Objects.requireNonNull(workflowId, "workflowId");

		DSLContext dsl = ensureDSL();

		return dsl.selectFrom(VN_NODE_VERSION).where(VN_NODE_VERSION.WORKFLOW_ID.eq(workflowId))
				.orderBy(VN_NODE_VERSION.TIMESTAMP.desc(), VN_NODE_VERSION.ID.desc())
				.fetch(this::nodeVersionToVersionMeta);
	}

	public List<VersionMeta> findByWorkflowIdAndCorrelationId(String workflowId, String correlationId) {
		Objects.requireNonNull(workflowId, "workflowId");
		Objects.requireNonNull(correlationId, "correlationId");

		DSLContext dsl = ensureDSL();

		return dsl.selectFrom(VN_NODE_VERSION)
				.where(VN_NODE_VERSION.WORKFLOW_ID.eq(workflowId).and(VN_NODE_VERSION.CORRELATION_ID.eq(correlationId)))
				.orderBy(VN_NODE_VERSION.TIMESTAMP.desc(), VN_NODE_VERSION.ID.desc())
				.fetch(this::nodeVersionToVersionMeta);
	}

	public List<VersionMeta> findByWorkflowIdAndCorrelationIdAndTransactionId(String workflowId, String correlationId,
			String transactionId) {
		Objects.requireNonNull(workflowId, "workflowId");
		Objects.requireNonNull(correlationId, "correlationId");
		Objects.requireNonNull(transactionId, "transactionId");

		DSLContext dsl = ensureDSL();

		return dsl.selectFrom(VN_NODE_VERSION)
				.where(VN_NODE_VERSION.CORRELATION_ID.eq(correlationId)
						.and(VN_NODE_VERSION.WORKFLOW_ID.eq(workflowId)
								.and(VN_NODE_VERSION.TRANSACTION_ID.eq(transactionId))))
				.orderBy(VN_NODE_VERSION.TIMESTAMP.desc(), VN_NODE_VERSION.ID.desc())
				.fetch(this::nodeVersionToVersionMeta);
	}

	public int saveAndPublishMeta(VersionMeta vm, Long inodeId) {
		Objects.requireNonNull(vm, "vm");
		Objects.requireNonNull(inodeId, "inodeId");

		// Insert the new version row, then move HEAD in".
		// Update is only allowed if the existing head is also unfenced.
		CommonTableExpression<Record2<Long, Long>> ins = name("ins").fields("version_id", "inode_id")
				.as(insertVersionMeta(vm, inodeId).returningResult(VN_NODE_VERSION.ID, VN_NODE_VERSION.INODE_ID));

		Field<Long> vId = field(name("ins", "version_id"), Long.class);
		Field<Long> iId = field(name("ins", "inode_id"), Long.class);
		Field<OffsetDateTime> now = currentOffsetDateTime();

		DSLContext dsl = ensureDSL();

		int rows = dsl.with(ins)
				.insertInto(VN_NODE_HEAD, VN_NODE_HEAD.INODE_ID, VN_NODE_HEAD.VERSION_ID, VN_NODE_HEAD.UPDATED_AT)
				.select(select(iId, vId, now).from(table(name("ins")))).onConflict(VN_NODE_HEAD.INODE_ID).doUpdate()
				.set(VN_NODE_HEAD.VERSION_ID, excluded(VN_NODE_HEAD.VERSION_ID)).set(VN_NODE_HEAD.UPDATED_AT, now)
				.execute();

		return rows;
	}

	List<VersionMeta> getWorkflows(Long inodeId) {

		DSLContext dsl = ensureDSL();
		return dsl.selectFrom(VN_NODE_VERSION)
				.where(VN_NODE_VERSION.WORKFLOW_ID.in(dsl.select(VN_NODE_VERSION.WORKFLOW_ID).from(VN_NODE_VERSION)
						.where(VN_NODE_VERSION.INODE_ID.eq(inodeId)).and(VN_NODE_VERSION.WORKFLOW_ID.isNotNull())))
				.orderBy(VN_NODE_VERSION.WORKFLOW_ID.asc(), VN_NODE_VERSION.TIMESTAMP.desc(), VN_NODE_VERSION.ID.desc())
				.fetch(this::nodeVersionToVersionMeta);
	}

	List<VersionMeta> getCorrelations(Long inodeId) {

		DSLContext dsl = ensureDSL();

		return dsl.selectFrom(VN_NODE_VERSION)
				.where(VN_NODE_VERSION.CORRELATION_ID.in(dsl.select(VN_NODE_VERSION.CORRELATION_ID)
						.from(VN_NODE_VERSION).where(VN_NODE_VERSION.INODE_ID.eq(inodeId))
						.and(VN_NODE_VERSION.WORKFLOW_ID.isNotNull())))
				.orderBy(VN_NODE_VERSION.WORKFLOW_ID.asc(), VN_NODE_VERSION.TIMESTAMP.desc(), VN_NODE_VERSION.ID.desc())
				.fetch(this::nodeVersionToVersionMeta);
	}

	List<VersionMeta> getTransactions(Long inodeId) {

		DSLContext dsl = ensureDSL();

		return dsl.selectFrom(VN_NODE_VERSION)
				.where(VN_NODE_VERSION.TRANSACTION_ID.in(dsl.select(VN_NODE_VERSION.TRANSACTION_ID)
						.from(VN_NODE_VERSION).where(VN_NODE_VERSION.INODE_ID.eq(inodeId))
						.and(VN_NODE_VERSION.WORKFLOW_ID.isNotNull())))
				.orderBy(VN_NODE_VERSION.WORKFLOW_ID.asc(), VN_NODE_VERSION.TIMESTAMP.desc(), VN_NODE_VERSION.ID.desc())
				.fetch(this::nodeVersionToVersionMeta);
	}

	private InsertSetMoreStep<VnNodeVersionRecord> insertVersionMeta(VersionMeta vm, Long inodeId) {
		DSLContext dsl = ensureDSL();

		return dsl.insertInto(VN_NODE_VERSION).set(VN_NODE_VERSION.INODE_ID, inodeId).
		// set(VN_NODE_VERSION.TIMESTAMP, DBTime.nowEpochMs()). set by DB
				set(VN_NODE_VERSION.PATH, vm.path()).set(VN_NODE_VERSION.OPERATION, vm.operation())
				.set(VN_NODE_VERSION.PRINCIPAL, vm.principal()).set(VN_NODE_VERSION.CORRELATION_ID, vm.correlationId())
				.set(VN_NODE_VERSION.WORKFLOW_ID, vm.workflowId()).set(VN_NODE_VERSION.CONTEXT_NAME, vm.contextName())
				.set(VN_NODE_VERSION.TRANSACTION_ID, vm.transactionId())
				.set(VN_NODE_VERSION.TRANSACTION_RESULT, vm.transactionResult())
				.set(VN_NODE_VERSION.HASH_ALGORITHM, vm.hashAlgorithm()).set(VN_NODE_VERSION.HASH, vm.hash())
				.set(VN_NODE_VERSION.NAME, vm.name()).set(VN_NODE_VERSION.MIME_TYPE, vm.mimeType())
				.set(VN_NODE_VERSION.SIZE, vm.size());
	}

	private VersionMeta fromHeadVersiontoVersionMeta(Record r) {
		Long size = r.get(VN_NODE_VERSION.SIZE);
		return new VersionMeta(r.get(VN_NODE_VERSION.HASH_ALGORITHM), r.get(VN_NODE_VERSION.HASH),
				r.get(VN_NODE_VERSION.NAME), r.get(VN_NODE_VERSION.MIME_TYPE), size == null ? 0L : size,
				r.get(VN_NODE_VERSION.PATH), r.get(VN_NODE_VERSION.TIMESTAMP), r.get(VN_NODE_VERSION.OPERATION),
				r.get(VN_NODE_VERSION.PRINCIPAL), r.get(VN_NODE_VERSION.CORRELATION_ID),
				r.get(VN_NODE_VERSION.WORKFLOW_ID), r.get(VN_NODE_VERSION.CONTEXT_NAME),
				r.get(VN_NODE_VERSION.TRANSACTION_ID), r.get(VN_NODE_VERSION.TRANSACTION_RESULT));
	}

	private VersionMeta nodeVersionToVersionMeta(VnNodeVersionRecord r) {
		Long size = r.getSize();
		return new VersionMeta(r.getHashAlgorithm(), r.getHash(), r.getName(), r.getMimeType(),
				size == null ? 0L : size,

				r.getPath(), r.getTimestamp(), // safe now (not null)
				r.getOperation(), r.getPrincipal(), r.getCorrelationId(), r.getWorkflowId(), r.getContextName(),
				r.getTransactionId(), r.getTransactionResult());
	}

}
