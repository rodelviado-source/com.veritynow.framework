package com.veritynow.core.store.versionstore;

import static com.veritynow.core.store.txn.TransactionResult.AUTO_COMMITTED;

import org.apache.tika.utils.StringUtils;

import com.veritynow.core.store.base.StoreContext;
import com.veritynow.core.store.base.StoreUtils;



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

		//When (epoch millis)
		//We consiously chosed epoch, since this is the most unambigous
		//representation of time
		//no timezone,dst etc.. common ambiguities present in other formats
		Long timestamp,

		// How Operation (store-level verb, "updated/created/updated/deleted/restored/")
		String operation,

		// Who (required, but can be anonymous)
		String principal,

		// Correlation (required)
		String correlationId,

		//used to scope a workflow
		String workflowId,

		// Context
		String contextName,
		
		// TransactionId use for (commit/rollback)
		String transactionId,
				
		//result of the transaction
		String transactionResult

) {
	public PathEvent {
		
		StoreUtils.enforceRequired(path, "path");
		StoreUtils.enforceRequired(operation, "operation");
		StoreUtils.enforceRequired(principal, "principal");
		StoreUtils.enforceRequired(workflowId, "workflowId");
		StoreUtils.enforceRequired(contextName, "contextName");
		
		if (!StringUtils.isEmpty(transactionId) && AUTO_COMMITTED.equals(transactionResult)) {
			throw new IllegalArgumentException("transactionId and transactionResult is out of sync " + transactionId + ":" + transactionResult );
		}
	}
	
	
	public PathEvent(String path, StoreContext sc) {
		this(
			StoreUtils.setRequired(path, "path"),
			//This is now set by DB during persistence,
			null,
			//Store context already validated upon creation
			sc.operation(), sc.principal(), sc.correlationId(), 
			sc.workflowId(), sc.contextName(), sc.transactionId(), sc.transactionResult() );
	}


}
