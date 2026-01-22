package com.veritynow.core.store.db;

import static com.veritynow.core.store.persistence.jooq.Tables.VN_INODE;
import static com.veritynow.core.store.persistence.jooq.Tables.VN_NODE_VERSION;

import java.util.List;
import java.util.Objects;

import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.veritynow.core.store.meta.VersionMeta;
import com.veritynow.core.store.persistence.jooq.tables.records.VnNodeVersionRecord;

import util.JSON;

public final class JooqVersionMetaRepository {

    private final DSLContext dsl;

    public JooqVersionMetaRepository(DSLContext dsl) {
        this.dsl = Objects.requireNonNull(dsl, "dsl");
    }

    public List<VersionMeta> findAllByInodeIdOrderByTimestampDescIdDesc(Long inodeId) {
        Objects.requireNonNull(inodeId, "inodeId");

        Result<Record> rows = dsl
            .select(VN_NODE_VERSION.fields())
            .select(VN_INODE.fields())
            .from(VN_NODE_VERSION)
            .join(VN_INODE).on(VN_INODE.ID.eq(VN_NODE_VERSION.INODE_ID))
            .where(VN_NODE_VERSION.INODE_ID.eq(inodeId))
            .orderBy(VN_NODE_VERSION.TIMESTAMP.desc(), VN_NODE_VERSION.ID.desc())
            .fetch();

        return rows.map(this::toVersionMeta);
    }

    public VersionMeta save(VersionMeta vm, Long inodeId) {
        Objects.requireNonNull(vm, "vme");
        Objects.requireNonNull(inodeId, "inodeId");
        
        try {
			System.out.println("===============================================================\n" + JSON.MAPPER.writeValueAsString(vm));
		} catch (JsonProcessingException e) {
			//ignore
		}
        VnNodeVersionRecord inserted = dsl
        	    .insertInto(VN_NODE_VERSION)
        	    .set(VN_NODE_VERSION.INODE_ID, inodeId)
        	    .set(VN_NODE_VERSION.TIMESTAMP, vm.timestamp())
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

        	return toVersionMeta(inserted);
 
    }

       
    
    private VersionMeta toVersionMeta(VnNodeVersionRecord r) {
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
    
    private VersionMeta toVersionMeta(Record r) {
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

}
