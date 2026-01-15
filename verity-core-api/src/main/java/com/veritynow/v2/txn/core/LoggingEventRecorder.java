package com.veritynow.v2.txn.core;

import com.veritynow.v2.txn.spi.EventRecorder;
import com.veritynow.v2.txn.spi.TxnContext;

import java.util.List;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Default EventRecorder for turn-key development experience.
 *
 * In production, applications typically override this with an implementation that persists PathEvents
 * into the store/event log. This implementation intentionally does not require any external infrastructure.
 */
public class LoggingEventRecorder implements EventRecorder {

	private static final Logger LOGGER = LogManager.getLogger();
	private static final String INFO = 
			"\n\tEvent Recorder started" + 
			"\n\tUsing {} as Event Recorder";
	
    public LoggingEventRecorder() {
		LOGGER.info(INFO, LoggingEventRecorder.class.getName());
	}

    @Override
    public void recordTxnBegan(TxnContext ctx) {
    	LOGGER.info(() -> "TxnBegan txnId=" + ctx.transactionId() + " lockRoot=" + ctx.lockRoot() + " lockId=" + ctx.lockId());
    }

    @Override
    public void recordCommitRequested(TxnContext ctx) {
    	LOGGER.info(() -> "TxnCommitRequested txnId=" + ctx.transactionId() + " lockRoot=" + ctx.lockRoot());
    }

    @Override
    public void recordTxnCommitted(TxnContext ctx, List<String> touchedPaths) {
    	LOGGER.info(() -> "TxnCommitted txnId=" + ctx.transactionId() + " touchedPaths=" + touchedPaths.size());
    }

    @Override
    public void recordTxnRolledBack(TxnContext ctx, String reason) {
        if (reason == null || reason.isBlank()) {
        	LOGGER.info(() -> "TxnRolledBack txnId=" + ctx.transactionId());
        } else {
        	LOGGER.warn("TxnRolledBack txnId={0} reason={1}", new Object[]{ctx.transactionId(), reason});
        }
    }

	

	
    
    
}
