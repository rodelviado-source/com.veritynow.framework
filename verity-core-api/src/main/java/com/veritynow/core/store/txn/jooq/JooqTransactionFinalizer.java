package com.veritynow.core.store.txn.jooq;

import static com.veritynow.core.store.persistence.jooq.Tables.VN_NODE_HEAD;
import static com.veritynow.core.store.persistence.jooq.Tables.VN_NODE_VERSION;
import static com.veritynow.core.store.txn.TransactionResult.COMMITTED;
import static com.veritynow.core.store.txn.TransactionResult.IN_FLIGHT;
import static com.veritynow.core.store.txn.TransactionResult.ROLLED_BACK;
import static org.jooq.impl.DSL.countDistinct;
import static org.jooq.impl.DSL.currentOffsetDateTime;
import static org.jooq.impl.DSL.excluded;
import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.inline;
import static org.jooq.impl.DSL.name;
import static org.jooq.impl.DSL.select;
import static org.jooq.impl.DSL.table;

import java.util.Objects;

import org.jooq.CommonTableExpression;
import org.jooq.DSLContext;
import org.jooq.InsertOnDuplicateStep;

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
    public void commit(String txnId) {
        Objects.requireNonNull(txnId, "txnId");
        // lockGroupId intentionally unused (interface parity)

        // We publish one HEAD movement per inode, even if multiple versions exist for the same inode within
        // a transaction (or if a buggy caller reuses the same txnId). This is both more correct and prevents
        // duplicate-conflict issues when inserting into vn_node_head.
        Long expectedBoxed = dsl.select(countDistinct(VN_NODE_VERSION.INODE_ID))
            .from(VN_NODE_VERSION)
            .where(VN_NODE_VERSION.TRANSACTION_ID.eq(txnId))
            .and(VN_NODE_VERSION.TRANSACTION_RESULT.eq(IN_FLIGHT))
            .fetchOne(0, Long.class);

        long expected = (expectedBoxed == null) ? 0L : expectedBoxed.longValue();
        if (expected == 0L) return; // idempotent

        // 1) Clone all IN_FLIGHT rows into COMMITTED (append-only semantics).
        // 2) Publish HEAD for the latest cloned version per inode using fencing.
        CommonTableExpression<?> cloned = name("cloned").as(
        		cloneAndSetTransactionResult(COMMITTED, txnId)
               .returning(VN_NODE_VERSION.INODE_ID, VN_NODE_VERSION.ID)
        );

        // "latest" = the single newest cloned version per inode
        var c = cloned.asTable().as("c");
        var cInodeId = c.field(VN_NODE_VERSION.INODE_ID);
        var cVersionId = c.field(VN_NODE_VERSION.ID);

        CommonTableExpression<?> latest = name("latest").as(
             dsl.select(cInodeId, cVersionId).distinctOn(cInodeId)
                .from(c)
                .orderBy(cInodeId.asc(), cVersionId.desc())
                
        );

        int published = dsl.with(cloned)
            .with(latest)
            .insertInto(VN_NODE_HEAD)
            .columns(
                VN_NODE_HEAD.INODE_ID,
                VN_NODE_HEAD.VERSION_ID,
                VN_NODE_HEAD.UPDATED_AT
            )
            .select(
                select(
                    field(name("latest", "inode_id"), Long.class),
                    field(name("latest", "id"), Long.class),
                    currentOffsetDateTime()
                )
                .from(table(name("latest")))
            )
            .onConflict(VN_NODE_HEAD.INODE_ID)
            .doUpdate()
            .set(VN_NODE_HEAD.VERSION_ID, excluded(VN_NODE_HEAD.VERSION_ID))
            .set(VN_NODE_HEAD.UPDATED_AT, excluded(VN_NODE_HEAD.UPDATED_AT))
            .execute();

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
