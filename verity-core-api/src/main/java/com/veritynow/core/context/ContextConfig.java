package com.veritynow.core.context;

/**
 * Configuration for MDC keys and behavior.
 */
public final class ContextConfig {

    public final String mdcCorrelationKey;
    public final String mdcTransactionKey;
    public final String mdcPrincipalKey;
    public final String mdcContextNameKey;
    public final String mdcWorkflowKey;

    private ContextConfig(Builder b) {
        this.mdcCorrelationKey = b.mdcCorrelationKey;
        this.mdcTransactionKey = b.mdcTransactionKey;
        this.mdcPrincipalKey = b.mdcPrincipalKey;
        this.mdcContextNameKey = b.mdcContextNameKey;
        this.mdcWorkflowKey = b.mdcWorkflowKey;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String mdcCorrelationKey = "trace_id";
        private String mdcTransactionKey = "txn_id";
        private String mdcPrincipalKey = "principal";
        private String mdcContextNameKey = "context_name";
        private String mdcWorkflowKey = "workflow_id";

        public Builder mdcCorrelationKey(String key) { this.mdcCorrelationKey = key; return this; }
        public Builder mdcTransactionKey(String key) { this.mdcTransactionKey = key; return this; }
        public Builder mdcPrincipalKey(String key) { this.mdcPrincipalKey = key; return this; }
        public Builder mdcContextNameKey(String key) { this.mdcContextNameKey = key; return this; };
        public Builder mdcWorkflowKey(String key) { this.mdcWorkflowKey = key; return this; };

        public ContextConfig build() { return new ContextConfig(this); }
    }
}
