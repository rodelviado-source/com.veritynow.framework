package com.veritynow.context;

import java.util.Optional;

/**
 * Mirrors context into MDC when available.
 */
final class MdcBridge {

    private final MdcAdapter mdc;
    private final ContextConfig cfg;

    MdcBridge(MdcAdapter mdc, ContextConfig cfg) {
        this.mdc = mdc;
        this.cfg = cfg;
    }

    boolean isAvailable() {
        return mdc != null && mdc.isAvailable();
    }

    Optional<ContextSnapshot> tryExtract() {
        if (!isAvailable()) return Optional.empty();
        String cid = mdc.get(cfg.mdcCorrelationKey).orElse(null);
        if (cid == null || cid.trim().isEmpty()) return Optional.empty();

        String txn = mdc.get(cfg.mdcTransactionKey).orElse(null);
        String wf = mdc.get(cfg.mdcWorkflowKey).orElse(null);
        String cn = mdc.get(cfg.mdcContextNameKey).orElse(null);
        String principal = mdc.get(cfg.mdcPrincipalKey).orElse(null);

        return Optional.of(ContextSnapshot.builder()
                .correlationId(cid)
                .workflowId(wf)
                .principal(principal)
                .transactionId(txn)
                .contextName(cn)
                .propagated(true)
                .build());
    }

    void apply(ContextSnapshot snap) {
        if (!isAvailable() || snap == null) return;

        mdc.put(cfg.mdcCorrelationKey, snap.correlationId());

        String txn = snap.transactionIdOrNull();
        if (txn != null) mdc.put(cfg.mdcTransactionKey, txn);
        else mdc.remove(cfg.mdcTransactionKey);

        String principal = snap.principalOrNull();
        if (principal != null) mdc.put(cfg.mdcPrincipalKey, principal);
        else mdc.remove(cfg.mdcPrincipalKey);
        
        String wf = snap.workflowIdOrNull();
        if (wf != null) mdc.put(cfg.mdcWorkflowKey, wf);
        else mdc.remove(cfg.mdcWorkflowKey);
        
        String cn = snap.contextNameOrNull();
        if (cn != null) mdc.put(cfg.mdcContextNameKey, cn);
        else mdc.remove(cfg.mdcContextNameKey);
    }

    void clear() {
        if (!isAvailable()) return;
        mdc.remove(cfg.mdcCorrelationKey);
        mdc.remove(cfg.mdcTransactionKey);
        mdc.remove(cfg.mdcPrincipalKey);
        mdc.remove(cfg.mdcContextNameKey);
        mdc.remove(cfg.mdcWorkflowKey);
        
    }
}
