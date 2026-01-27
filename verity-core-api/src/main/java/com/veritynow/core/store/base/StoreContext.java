package com.veritynow.core.store.base;

import static com.veritynow.core.store.txn.TransactionResult.AUTO_COMMITTED;
import static com.veritynow.core.store.txn.TransactionResult.IN_FLIGHT;

import java.util.UUID;

import com.veritynow.core.context.Context;
import com.veritynow.core.context.ContextSnapshot;

import util.StringUtils;

public record StoreContext(
		String principal, String correlationId, String workflowId, String operation,
		String contextName, String transactionId, String transactionResult

) {


	
	public final static String ANONYMOUS = "anonymous";
		
	public StoreContext {
		
	StoreUtils.enforceRequired(principal, "principal");
	StoreUtils.enforceRequired(correlationId, "correlationId");
	StoreUtils.enforceRequired(workflowId, "workflowId");
	StoreUtils.enforceRequired(operation, "operation");
	StoreUtils.enforceRequired(contextName, "contextName");
	StoreUtils.enforceRequired(transactionResult, "transactionResult");
	
	if (StringUtils.isEmpty(transactionId)) {
	    if (!AUTO_COMMITTED.equals(transactionResult)) {
	        throw new IllegalArgumentException("transactionId missing but transactionResult=" + transactionResult);
	    }
	} else {
	    if (AUTO_COMMITTED.equals(transactionResult)) {
	        throw new IllegalArgumentException("transactionId present but transactionResult=" + AUTO_COMMITTED);
	    }
	}
	
	
	}

	public StoreContext(ContextSnapshot snap, String operation) {
		this(   StoreUtils.setOrDefault(snap.principalOrNull(), ANONYMOUS),
				StoreUtils.setRequired(snap.correlationId(), "correlationId"),
				StoreUtils.setOrDefault(snap.workflowIdOrNull(), snap.correlationId()),
				StoreUtils.setRequired(operation, "operation"),
				StoreUtils.setOrDefault(snap.contextNameOrNull(), operation),
				snap.transactionIdOrNull(),
				snap.transactionIdOrNull() == null ? AUTO_COMMITTED : IN_FLIGHT
				);
	}
	
	public StoreContext(ContextSnapshot snap, String operation, String transactionResult) {
		this(   StoreUtils.setOrDefault(snap.principalOrNull(), ANONYMOUS),
				StoreUtils.setRequired(snap.correlationId(), "correlationId"),
				StoreUtils.setOrDefault(snap.workflowIdOrNull(), snap.correlationId()),
				StoreUtils.setRequired(operation, "operation"),
				StoreUtils.setOrDefault(snap.contextNameOrNull(), operation),
				snap.transactionIdOrNull(),
				snap.transactionIdOrNull() == null ? AUTO_COMMITTED : transactionResult == null ? IN_FLIGHT : transactionResult 
				);
	}
	
	public static StoreContext create(String operation) {
		if (Context.isActive()) {
			return new StoreContext(Context.snapshot(),operation); 
		}
		String cid = UUID.randomUUID().toString();
		return new StoreContext(ANONYMOUS, cid,cid,operation, operation,null, AUTO_COMMITTED);
	}

	
}
