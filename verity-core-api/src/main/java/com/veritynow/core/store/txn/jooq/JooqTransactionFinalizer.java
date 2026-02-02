package com.veritynow.core.store.txn.jooq;

import static com.veritynow.core.store.persistence.jooq.Tables.VN_NODE_HEAD;
import static com.veritynow.core.store.persistence.jooq.Tables.VN_NODE_VERSION;
import static com.veritynow.core.store.txn.TransactionResult.COMMITTED;
import static com.veritynow.core.store.txn.TransactionResult.IN_FLIGHT;
import static com.veritynow.core.store.txn.TransactionResult.ROLLED_BACK;
import static org.jooq.impl.DSL.excluded;
import static org.jooq.impl.DSL.name;
import static org.jooq.impl.DSL.select;

import java.util.Objects;

import org.jooq.CommonTableExpression;
import org.jooq.DSLContext;

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
        
        var finalized = finalizedVersions(txnId);
        // "latest" = the single newest version per inode
        var committed = finalized.asTable();
        var inodeId = committed.field(VN_NODE_VERSION.INODE_ID);
        var id = committed.field(VN_NODE_VERSION.ID);

        CommonTableExpression<?> latest = name("latest").as(
             dsl.select(inodeId, id).distinctOn(inodeId)
                .from(committed)
                .orderBy(inodeId.asc(), id.desc())
        );
        
         
        var publisher = dsl.with(finalized)
            .with(latest)
            //Insert - assume no heads exist, onConflicet do an  update
            .insertInto(VN_NODE_HEAD)
            .columns(
                VN_NODE_HEAD.INODE_ID,
                VN_NODE_HEAD.VERSION_ID
            )
            .select(
                select(
                	latest.field(VN_NODE_VERSION.INODE_ID),
                	latest.field(VN_NODE_VERSION.ID)
                )
                .from(latest)
              )
      
            .onConflict(VN_NODE_HEAD.INODE_ID)
            .doUpdate()
            .set(VN_NODE_HEAD.VERSION_ID, excluded(VN_NODE_HEAD.VERSION_ID))
            .set(VN_NODE_HEAD.UPDATED_AT, excluded(VN_NODE_HEAD.UPDATED_AT));
        
         var forUpdate = selectForUpdate(txnId);
	     
         
         int expected = dsl.with(forUpdate).selectFrom(forUpdate).execute();
         if (expected == 0) return;
	     int published = publisher.execute();
	     
        if (expected != published) {
            throw new IllegalStateException(
                "HEAD publish rejected : expected=" + expected + " published=" + published
            );
        }
        dsl.commit();
    }

    @Override
    public void rollback(String txnId) {
    	dsl.rollback();
//        Objects.requireNonNull(txnId, "txnId");
//        // Single statement:  IN_FLIGHT -> ROLLED_BACK. No head movement.
//        var forUpdate = selectForUpdate(txnId);
//        int expected = dsl.with(forUpdate).selectFrom(forUpdate).execute();
//        if (expected == 0) return;
//        dsl.
//		update(VN_NODE_VERSION).
//		set(VN_NODE_VERSION.TRANSACTION_RESULT, ROLLED_BACK).
//		where(	
//				VN_NODE_VERSION.TRANSACTION_ID.eq(txnId).
//				and(
//				VN_NODE_VERSION.TRANSACTION_RESULT.eq(IN_FLIGHT))
//		).execute();
    }
    
    
    private CommonTableExpression<VnNodeVersionRecord> finalizedVersions(String txnId ) {
    	return name("finalized").as(
    			dsl.
				update(VN_NODE_VERSION).
				set(VN_NODE_VERSION.TRANSACTION_RESULT, COMMITTED).
				where(	
						VN_NODE_VERSION.TRANSACTION_ID.eq(txnId).
						and(
						VN_NODE_VERSION.TRANSACTION_RESULT.eq(IN_FLIGHT))
				).
                returning(VN_NODE_VERSION.INODE_ID, VN_NODE_VERSION.ID)
        );
    }
    
    
    private  CommonTableExpression<?> selectForUpdate(String txnId) {
    	
    	CommonTableExpression<?> locked = name("locked").as(
    			dsl.select(VN_NODE_VERSION.TRANSACTION_ID).from(VN_NODE_VERSION)
    			.where(	VN_NODE_VERSION.TRANSACTION_ID.eq(txnId)
    					.and(VN_NODE_VERSION.TRANSACTION_RESULT.eq(IN_FLIGHT))).forUpdate().noWait()
        );
    	
    	
		return locked;
    }
    
}
