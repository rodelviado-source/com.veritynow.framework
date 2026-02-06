package com.veritynow.core.store.txn.jooq;

import static com.veritynow.core.store.persistence.jooq.Tables.VN_NODE_HEAD;
import static com.veritynow.core.store.persistence.jooq.Tables.VN_NODE_VERSION;
import static com.veritynow.core.store.txn.TransactionResult.COMMITTED;
import static com.veritynow.core.store.txn.TransactionResult.IN_FLIGHT;
import static com.veritynow.core.store.txn.TransactionResult.ROLLED_BACK;
import static org.jooq.impl.DSL.excluded;
import static org.jooq.impl.DSL.name;
import static org.jooq.impl.DSL.select;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.util.Objects;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jooq.CommonTableExpression;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

import com.veritynow.core.store.persistence.jooq.tables.records.VnNodeVersionRecord;
import com.veritynow.core.store.txn.TransactionContext;
import com.veritynow.core.store.txn.TransactionFinalizer;

/**
 * TransactionFinalizer implemented with jOOQ generated tables using set-based SQL.
 *
 * commit(): single statement via data-modifying CTE:
 *   - versions IN_FLIGHT -> COMMITTED (RETURNING inode_id, version_id)
 *   - publish HEAD using INSERT..SELECT..ON CONFLICT
 *
 * rollback(): single statement:
 *   - IN_FLIGHT -> ROLLED_BACK
 *   - no head movement
 */
public class JooqTransactionFinalizer implements TransactionFinalizer {
	final static Logger  LOGGER = LogManager.getLogger(); 
    

    private DSLContext ensureDSL(String txnId) {
    	Connection conn = TransactionContext.getConnection(txnId);
    	if (conn == null) {
    		throw new IllegalStateException("Finalizer called without an active transaction. txnId = " + txnId);
    	}
    	
    	return DSL.using(conn, SQLDialect.POSTGRES);
    }
    
    
    
    @Override
	public void begin(String txnId) {
    	Objects.requireNonNull(txnId, "txnId");
    	 
    	 Connection conn = TransactionContext.getConnection(txnId);
    	 //create a savepoint so rollback can transition the versions
    	 //from in_flight to rolled_back without the head movement
    	 try { TransactionContext.putSavepoint(txnId, conn.setSavepoint());} catch (Exception e) {
    		 throw  new IllegalStateException("Unable to set savepoint ",e); 
    	};
	}



	@Override
    public void commit(String txnId) {
        Objects.requireNonNull(txnId, "txnId");
        
        Connection conn = TransactionContext.getConnection(txnId);
        
        if (conn == null) {
        	 throw  new IllegalStateException("commit called with no active transaction txnId = " + txnId);
        }
        
        DSLContext  dsl = ensureDSL(txnId);
        
        var finalized = finalizedVersions(txnId, dsl);
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
        
        	 
        	 Savepoint sp = TransactionContext.getSavepoint(txnId);
        	 
        	//realease the previous savepoint set by begin()
         	 if (sp != null) {
        		 try {	conn.releaseSavepoint(sp); } catch (Exception e) {
            		 throw  new IllegalStateException("Unable to release savepoint ",e); 
             	};
        	 }
        	 
        	//create a savepoint so rollback can transition the versions
        	 //from in_flight to rolled_back without the head movement
        	 try { TransactionContext.putSavepoint(txnId, conn.setSavepoint());} catch (Exception e) {
        		 throw  new IllegalStateException("Unable to set savepoint ",e); 
        	};
	         
		     publisher.execute();

        //important do not cleanup here on exception
        //let rollback record the rollback
        //cleanup on success only
        try  {
      		conn.commit();
      		TransactionContext.clear(txnId);
        	dsl = null;
		} catch (SQLException e) {
			rollback(txnId);
		} 
        
    }

    @Override
    public void rollback(String txnId) {
        Objects.requireNonNull(txnId, "txnId");
        Connection conn = TransactionContext.getConnection(txnId);
        Savepoint sp = TransactionContext.getSavepoint(txnId);
        
        if (conn == null) return;
        
        DSLContext  dsl = ensureDSL(txnId);
        
        if (sp != null) {
        	try {
        		TransactionContext.removeSavepoint(txnId);
				conn.rollback(sp);
				conn.releaseSavepoint(sp);
			} catch (SQLException e) {
				dsl = null;
				TransactionContext.clear(txnId);
				throw new IllegalStateException("rollback failed txnId",  e);
			}
        }
        
        // Single statement:  IN_FLIGHT -> ROLLED_BACK. No head movement.
        try (conn) {
	        
	        dsl.
			update(VN_NODE_VERSION).
			set(VN_NODE_VERSION.TRANSACTION_RESULT, ROLLED_BACK).
			where(	
					VN_NODE_VERSION.TRANSACTION_ID.eq(txnId).
					and(
					VN_NODE_VERSION.TRANSACTION_RESULT.in(IN_FLIGHT, COMMITTED))
			).execute();
	
	        conn.commit();
        }         
        catch (SQLException e) {
        	LOGGER.error("rollback failed txnId = {}", txnId, e);
        	throw new IllegalStateException("rollback failed txnId = {}",  e);
		
		} finally {
			dsl = null;
			TransactionContext.clear(txnId);
		}	
    }
    
    
    private CommonTableExpression<VnNodeVersionRecord> finalizedVersions(String txnId, DSLContext dsl ) {
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
    
}
