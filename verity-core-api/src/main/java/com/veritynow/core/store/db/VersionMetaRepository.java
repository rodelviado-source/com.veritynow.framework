package com.veritynow.core.store.db;

import static com.veritynow.core.store.persistence.jooq.Tables.VN_NODE_HEAD;
import static com.veritynow.core.store.persistence.jooq.Tables.VN_NODE_VERSION;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.jooq.DSLContext;
import org.jooq.Record;

import com.veritynow.core.store.meta.VersionMeta;
import com.veritynow.core.store.persistence.jooq.tables.records.VnNodeVersionRecord;

public final class VersionMetaRepository {

    private final DSLContext dsl;

    public VersionMetaRepository(DSLContext dsl) {
        this.dsl = Objects.requireNonNull(dsl, "dsl");
    }

    public List<VersionMeta> findAllByInodeIdOrderByTimestampDescIdDesc(Long inodeId) {
        Objects.requireNonNull(inodeId, "inodeId");

        return dsl
            .selectFrom(VN_NODE_VERSION)
            .where(VN_NODE_VERSION.INODE_ID.eq(inodeId))
            .orderBy(VN_NODE_VERSION.TIMESTAMP.desc(), VN_NODE_VERSION.ID.desc())
            .fetch(this::nodeVersionToVersionMeta);
    }

    public VersionMeta save(VersionMeta vm, Long inodeId) {
        Objects.requireNonNull(vm, "vme");
        Objects.requireNonNull(inodeId, "inodeId");
        
        VnNodeVersionRecord inserted = dsl
        	    .insertInto(VN_NODE_VERSION)
        	    .set(VN_NODE_VERSION.INODE_ID, inodeId)
        	    .set(VN_NODE_VERSION.TIMESTAMP, DBTime.nowEpochMs())
        	    .set(VN_NODE_VERSION.PATH, vm.path())
        	    .set(VN_NODE_VERSION.OPERATION, vm.operation())
        	    .set(VN_NODE_VERSION.PRINCIPAL, vm.principal())
        	    .set(VN_NODE_VERSION.CORRELATION_ID, vm.correlationId())
        	    .set(VN_NODE_VERSION.WORKFLOW_ID, vm.workflowId())
        	    .set(VN_NODE_VERSION.CONTEXT_NAME, vm.contextName())
        	    .set(VN_NODE_VERSION.TRANSACTION_ID, vm.transactionId())
        	    .set(VN_NODE_VERSION.TRANSACTION_RESULT, vm.transactionResult())
        	    .set(VN_NODE_VERSION.HASH, vm.hash())
        	    .set(VN_NODE_VERSION.NAME, vm.name())
        	    .set(VN_NODE_VERSION.MIME_TYPE, vm.mimeType())
        	    .set(VN_NODE_VERSION.SIZE, vm.size())
        	    .returning(VN_NODE_VERSION.fields())   // <-- key change
        	    .fetchOneInto(VnNodeVersionRecord.class);

        	if (inserted == null || inserted.getId() == null) {
        	    throw new IllegalStateException("Insert into vn_node_version did not return an id");
        	}

        	return nodeVersionToVersionMeta(inserted);
 
    }

    public Optional<VersionMeta> findLatestVersionByInodeId(Long inodeId) {
        Objects.requireNonNull(inodeId, "inodeId");

        return dsl
            .select(VN_NODE_VERSION.fields())
            .from(VN_NODE_HEAD)
            .join(VN_NODE_VERSION)
                .on(VN_NODE_VERSION.ID.eq(VN_NODE_HEAD.VERSION_ID))
            .where(VN_NODE_HEAD.INODE_ID.eq(inodeId))
            .fetchOptional(this::fromHeadVersiontoVersionMeta); // mapper reads VN_NODE_VERSION only
    }
    
    
    
    private VersionMeta fromHeadVersiontoVersionMeta(Record r) {
        Long size = r.get(VN_NODE_VERSION.SIZE);
        return new VersionMeta(
            r.get(VN_NODE_VERSION.HASH),
            r.get(VN_NODE_VERSION.NAME),
            r.get(VN_NODE_VERSION.MIME_TYPE),
            size == null ? 0L : size,
            r.get(VN_NODE_VERSION.PATH),
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
    
    private VersionMeta nodeVersionToVersionMeta(VnNodeVersionRecord r) {
        Long size = r.getSize();
        return new VersionMeta(
            r.getHash(),
            r.getName(),
            r.getMimeType(),
            size == null ? 0L : size,

            r.getPath(),
            r.getTimestamp(),          // safe now (not null)
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
