package com.veritynow.context;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Immutable context snapshot.
 */
public final class ContextSnapshot {

    private final String correlationId;     // required
    private final String transactionId;     // optional
    private final String principal;         // optional
    private final String contextName;       // optional
    private final boolean propagated;
    private final Map<String, String> tags; // immutable

    private ContextSnapshot(Builder b) {
        this.correlationId = requireNonBlank(b.correlationId, "correlationId");
        this.transactionId = blankToNull(b.transactionId);
        this.principal = blankToNull(b.principal);
        this.contextName = blankToNull(b.contextName);
        this.propagated = b.propagated;
        this.tags = Collections.unmodifiableMap(new HashMap<>(b.tags));
    }

    public String correlationId() { return correlationId; }
    public String transactionIdOrNull() { return transactionId; }
    public String principalOrNull() { return principal; }
    public String contextNameOrNull() { return contextName; }
    public boolean propagated() { return propagated; }
    public Map<String, String> tags() { return tags; }

    public static Builder builder() {
        return new Builder();
    }

    public Builder toBuilder() {
        return new Builder()
                .correlationId(correlationId)
                .transactionId(transactionId)
                .principal(principal)
                .contextName(contextName)
                .propagated(propagated)
                .tags(tags);
    }

    private static String requireNonBlank(String s, String name) {
        if (s == null || s.trim().isEmpty()) throw new IllegalArgumentException(name + " must not be blank");
        return s.trim();
    }

    private static String blankToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    public static final class Builder {
        private String correlationId;
        private String transactionId;
        private String principal;
        private String contextName;
        private boolean propagated = false;
        private Map<String, String> tags = new HashMap<>();

        public Builder correlationId(String correlationId) { this.correlationId = correlationId; return this; }
        public Builder transactionId(String transactionId) { this.transactionId = transactionId; return this; }
        public Builder principal(String principal) { this.principal = principal; return this; }
        public Builder contextName(String contextName) { this.contextName = contextName; return this; }
        public Builder propagated(boolean propagated) { this.propagated = propagated; return this; }

        public Builder putTag(String k, String v) {
            if (k == null || k.trim().isEmpty()) throw new IllegalArgumentException("tag key must not be blank");
            if (v == null) throw new IllegalArgumentException("tag value must not be null");
            tags.put(k.trim(), v);
            return this;
        }

        public Builder tags(Map<String, String> tags) {
            this.tags.clear();
            if (tags != null) this.tags.putAll(tags);
            return this;
        }

        public ContextSnapshot build() {
            return new ContextSnapshot(this);
        }
    }
}
