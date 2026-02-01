package com.veritynow.core.store.txn.jooq;

import java.util.Objects;

import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.veritynow.core.context.Context;
import com.veritynow.core.context.ContextScope;
import com.veritynow.core.store.txn.TransactionFinalizer;
import com.veritynow.core.store.txn.TransactionService;

/**
 * jOOQ TransactionService backed by vn_txn_epoch.
 *
 * No JPA entities or repositories.
 */
public class JooqTransactionService implements TransactionService<ContextScope> {

	private final TransactionFinalizer finalizer;
   
 
    public JooqTransactionService(TransactionFinalizer finalizer) {
    	Objects.requireNonNull(finalizer);
    	this.finalizer = finalizer;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public ContextScope begin(String txnId) {
        Objects.requireNonNull(txnId, "txnId");
        if (!Context.isActive()) {
        	Context.ensureContext("Transaction(begin) Contex");
        }
        
        return Context.scope();
        
        //Get current context for txnId, if missing make one
        //Final state we don't need the txnId passed
    } 
    

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void commit(String txnId) {
        Objects.requireNonNull(txnId, "txnId");
       //Get current context, throw exception if missing
       //Final state we don't need the txnId passed
        if (!Context.isActive()) {
        	throw new RuntimeException("Commit called without an active context, call begin() or create a context");
        }
        finalizer.commit(txnId);
        if ("Transaction(begin) Contex".equals(Context.contextNameOrNull())) {
        	Context.scope().close();
        }
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void rollback(String txnId) {
        Objects.requireNonNull(txnId, "txnId");
        //Get current context, throw exception if missing
        //Final state we don't need the txnId passed
        if (!Context.isActive()) {
        	throw new RuntimeException("Rollback called without an active context, call begin() or create a context");
        }
        
        finalizer.rollback(txnId);
        if ("Transaction(begin) Contex".equals(Context.contextNameOrNull())) {
        	Context.scope().close();
        }
       
    }
    
}
