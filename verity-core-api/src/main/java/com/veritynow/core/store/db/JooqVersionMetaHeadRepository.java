package com.veritynow.core.store.db;

import static com.veritynow.core.store.persistence.jooq.Tables.VN_INODE;
import static com.veritynow.core.store.persistence.jooq.Tables.VN_NODE_HEAD;
import static com.veritynow.core.store.persistence.jooq.Tables.VN_NODE_VERSION;

import java.util.Objects;
import java.util.Optional;

import org.jooq.DSLContext;
import org.jooq.Record;

import com.veritynow.core.store.meta.VersionMeta;

public final class JooqVersionMetaHeadRepository {

    private final DSLContext dsl;

    public JooqVersionMetaHeadRepository(DSLContext dsl) {
        this.dsl = Objects.requireNonNull(dsl, "dsl");
    }

    
    public Optional<VersionMeta> findLatestVersionByInodeId(Long inodeId) {
        Objects.requireNonNull(inodeId, "inodeId");

		return dsl
            .select(
                VN_NODE_HEAD.fields()
            )
            .select(
                VN_NODE_VERSION.fields()
            )
            .select(
                VN_INODE.fields()
            )
            .from(VN_NODE_HEAD)
            .join(VN_NODE_VERSION)
                .on(VN_NODE_VERSION.ID.eq(VN_NODE_HEAD.VERSION_ID))
            .join(VN_INODE)
                .on(VN_INODE.ID.eq(VN_NODE_HEAD.INODE_ID))
            .where(VN_NODE_HEAD.INODE_ID.eq(inodeId))
            .fetchOptional(this::toVersionMeta);
		
    }
    
    
    
    private VersionMeta toVersionMeta(Record r) {
        if (r.get(VN_INODE.CREATED_AT) == null) {
            throw new IllegalStateException("vn_inode.created_at is NULL (violates schema invariant)");
        }
       
       
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
