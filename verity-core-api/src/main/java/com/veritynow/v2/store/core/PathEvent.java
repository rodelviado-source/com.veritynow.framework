package com.veritynow.v2.txn.core;

import org.threeten.bp.Instant;

import com.veritynow.v2.store.core.StoreContext;
import com.veritynow.v2.store.core.StoreUtils;


/**
 * Canonical, transport-agnostic event record.
 *
 * Notes: - This type is intended to survive across transports (HTTP, offline,
 * messaging). - Avoid transport semantics (headers, URLs, status codes) in this
 * record. - Schema evolution should be additive only (new optional fields).
 */
public record PathEvent(

		// What path was affected (absolute, namespace-qualified)
		String path,

		// When (epoch millis)
		long timestamp,

		// How Operation (domain-level verb; not necessarily an HTTP verb)
		String operation,

		// Who (required, but can be anonymous)
		String principal,

		// Correlation (required)
		String correlationId,

		// TransactionId
		String transactionId,

		// Context
		String contextName

) {
	public PathEvent {
		
		StoreUtils.setRequired(path, "path");
		StoreUtils.setRequired(operation, "operation");
		StoreUtils.setOrDefault(principal, "anonymous");
		StoreUtils.setOrDefault(transactionId, correlationId);
		StoreUtils.setOrDefault(contextName, operation);
		
	}
	
	
	public PathEvent(String path, StoreContext sc) {
		this(
			StoreUtils.setRequired(path, "path"),	
			Instant.now().toEpochMilli(),
			//Store context already validated upon creation
			sc.operation(), sc.principal(), sc.correlationId(), 
			sc.transactionId(), sc.contextName());
	}


}
