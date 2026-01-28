package com.veritynow.core.store.txn.jooq;

import static com.veritynow.core.store.persistence.jooq.Tables.VN_NODE_HEAD;
import static com.veritynow.core.store.persistence.jooq.Tables.VN_NODE_VERSION;
import static com.veritynow.core.store.txn.TransactionResult.COMMITTED;
import static com.veritynow.core.store.txn.TransactionResult.IN_FLIGHT;
import static com.veritynow.core.store.txn.TransactionResult.ROLLED_BACK;
import static org.jooq.impl.DSL.excluded;
import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.inline;
import static org.jooq.impl.DSL.name;
import static org.jooq.impl.DSL.select;
import static org.jooq.impl.DSL.table;
import static org.jooq.impl.DSL.val;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

import org.jooq.CommonTableExpression;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.InsertOnDuplicateStep;
import org.jooq.Record2;

import com.veritynow.core.store.persistence.jooq.tables.records.VnNodeVersionRecord;
import com.veritynow.core.store.txn.TransactionFinalizer;

/**
 * TransactionFinalizer implemented with jOOQ generated tables using set-based SQL.
 *
 * commit(): single statement via data-modifying CTE:
 *   - clone IN_FLIGHT -> COMMITTED (RETURNING inode_id, version_id)
 *   - publish HEAD using INSERT..SELECT..ON CONFLICT..WHERE fence predicate
 *
 * rollback(): single statement:
 *   - clone IN_FLIGHT -> ROLLED_BACK
 *   - no head movement
 */
public class JooqTransactionFinalizer implements TransactionFinalizer {

    private final DSLContext dsl;

    public JooqTransactionFinalizer(DSLContext dsl) {
        this.dsl = Objects.requireNonNull(dsl, "dsl");
    }

    @Override
    public void commit(String txnId, UUID lockGroupId, long fenceToken) {
        Objects.requireNonNull(txnId, "txnId");
        // lockGroupId intentionally unused (interface parity)

        long expected = dsl.fetchCount(
            VN_NODE_VERSION,
            VN_NODE_VERSION.TRANSACTION_ID.eq(txnId)
                .and(VN_NODE_VERSION.TRANSACTION_RESULT.eq(IN_FLIGHT))
        );

        // Idempotent commit
        if (expected == 0) {
            return;
        }
              
        // Data-modifying CTE that clones IN_FLIGHT -> COMMITTED and returns (inode_id, version_id)
        CommonTableExpression<Record2<Long, Long>> cloned =
        	    name("cloned")
        	       .fields("inode_id", "version_id")
        	       .as(
        	    		cloneAndSetTransactionResult(COMMITTED, txnId)
        	            // IMPORTANT: returningResult gives Record2<Long,Long> (inode_id, version_id)
        	           .returningResult(VN_NODE_VERSION.INODE_ID, VN_NODE_VERSION.ID)
        	       );

        // Server-side updated_at (timestamptz -> OffsetDateTime)
        
        Field<OffsetDateTime> updatedAtNow = field("now()", OffsetDateTime.class);
        // Reference the returned CTE columns in a stable way
        
        var c = cloned.asTable();
        Field<Long> cInodeId   = c.field("inode_id", Long.class);
        Field<Long> cVersionId = c.field("version_id", Long.class);

        // Single statement: clone + publish
        int published = dsl
            .with(cloned)
            .insertInto(
                VN_NODE_HEAD,
                VN_NODE_HEAD.INODE_ID,
                VN_NODE_HEAD.VERSION_ID,
                VN_NODE_HEAD.UPDATED_AT,
                VN_NODE_HEAD.FENCE_TOKEN
            )
            .select(
                select(
                    cInodeId,
                    cVersionId,
                    updatedAtNow,
                    val(fenceToken)
                ).from(table(name("cloned")))
            )
            .onConflict(VN_NODE_HEAD.INODE_ID)
            .doUpdate()
            .set(VN_NODE_HEAD.VERSION_ID, excluded(VN_NODE_HEAD.VERSION_ID))
            .set(VN_NODE_HEAD.UPDATED_AT, excluded(VN_NODE_HEAD.UPDATED_AT))
            .set(VN_NODE_HEAD.FENCE_TOKEN, excluded(VN_NODE_HEAD.FENCE_TOKEN))
            .where(VN_NODE_HEAD.FENCE_TOKEN.lt(excluded(VN_NODE_HEAD.FENCE_TOKEN)))
            .execute();

        // In the success case, every cloned inode row must publish exactly once.
        // If fencing rejects any row, published < expected.
        if (published != expected) {
            throw new IllegalStateException(
                "HEAD publish rejected due to fencing: expected=" + expected + " published=" + published
            );
        }
    }

    @Override
    public void rollback(String txnId) {
        Objects.requireNonNull(txnId, "txnId");
        

        // Single statement: clone IN_FLIGHT -> ROLLED_BACK. No head movement.
        cloneAndSetTransactionResult(ROLLED_BACK, txnId).execute();
    }
    
    
    
    private InsertOnDuplicateStep<VnNodeVersionRecord> cloneAndSetTransactionResult(String result, String txnId) {
    	//Field<Long> nowMs = DBTime.nowEpochMs();

    	return dsl.insertInto(
                VN_NODE_VERSION,
                VN_NODE_VERSION.INODE_ID,
               // VN_NODE_VERSION.TIMESTAMP, //set by DB
                VN_NODE_VERSION.PATH,
                VN_NODE_VERSION.OPERATION,
                VN_NODE_VERSION.PRINCIPAL,
                VN_NODE_VERSION.CORRELATION_ID,
                VN_NODE_VERSION.WORKFLOW_ID,
                VN_NODE_VERSION.CONTEXT_NAME,
                VN_NODE_VERSION.TRANSACTION_ID,
                VN_NODE_VERSION.TRANSACTION_RESULT,
                VN_NODE_VERSION.HASH_ALGORITHM,
                VN_NODE_VERSION.HASH,
                VN_NODE_VERSION.NAME,
                VN_NODE_VERSION.MIME_TYPE,
                VN_NODE_VERSION.SIZE
            )
            .select(
                select(
                    VN_NODE_VERSION.INODE_ID,
                   // nowMs,  //set by DB
                    VN_NODE_VERSION.PATH,
                    VN_NODE_VERSION.OPERATION,
                    VN_NODE_VERSION.PRINCIPAL,
                    VN_NODE_VERSION.CORRELATION_ID,
                    VN_NODE_VERSION.WORKFLOW_ID,
                    VN_NODE_VERSION.CONTEXT_NAME,
                    VN_NODE_VERSION.TRANSACTION_ID,
                    inline(result),
                    VN_NODE_VERSION.HASH_ALGORITHM,
                    VN_NODE_VERSION.HASH,
                    VN_NODE_VERSION.NAME,
                    VN_NODE_VERSION.MIME_TYPE,
                    VN_NODE_VERSION.SIZE
                )
                .from(VN_NODE_VERSION)
                .where(
                    VN_NODE_VERSION.TRANSACTION_ID.eq(txnId)
                        .and(VN_NODE_VERSION.TRANSACTION_RESULT.eq(IN_FLIGHT))
                )
           	);
    }
}
