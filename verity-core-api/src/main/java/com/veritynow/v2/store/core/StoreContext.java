package com.veritynow.v2.store.core;

import com.veritynow.context.ContextSnapshot;
import com.veritynow.v2.txn.core.PathEvent;

public record StoreContext(String principal, String correlationId, String transactionId, String operation,
		String contextName

) {

	public StoreContext {
		
	StoreUtils.setOrDefault(principal, "anonymous");
	StoreUtils.setRequired(correlationId, "correlationId");
	StoreUtils.setOrDefault(transactionId, correlationId);
	StoreUtils.setRequired(operation, "operation");
	StoreUtils.setOrDefault(contextName, operation);
		
	}

	public StoreContext(ContextSnapshot snap, String operation) {
		this(StoreUtils.setOrDefault(snap.principalOrNull(), "anonymous"),
				StoreUtils.setRequired(snap.correlationId(), "correlationId"),
				StoreUtils.setOrDefault(snap.transactionIdOrNull(), snap.correlationId()),
				StoreUtils.setRequired(operation, "operation"),
				StoreUtils.setOrDefault(snap.contextNameOrNull(), operation));
	}

	public StoreContext(PathEvent pe) {
		this(StoreUtils.setOrDefault(pe.principal(), "anonymous"),
				StoreUtils.setRequired(pe.correlationId(), "correlationId"),
				StoreUtils.setOrDefault(pe.transactionId(), pe.correlationId()),
				StoreUtils.setRequired(pe.operation(), "operation"),
				StoreUtils.setOrDefault(pe.contextName(), pe.operation()));
	}

}
