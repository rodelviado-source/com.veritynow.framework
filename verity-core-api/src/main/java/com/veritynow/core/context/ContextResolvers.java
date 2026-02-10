package com.veritynow.core.context;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

public final class ContextResolvers {

    private ContextResolvers() {}

    // ---- Header keys (canonical) ----
    public static final String HDR_TRANSACTION_ID = "X-Transaction-Id";
    public static final String HDR_CORRELATION_ID = "X-Correlation-Id";
    public static final String HDR_WORKFLOW_ID    = "X-Workflow-Id";
    public static final String HDR_PRINCIPAL      = "X-Principal";
    public static final String HDR_CONTEXT_NAME   = "X-Context-Name";

    // ---- HTTP ----

    public static ContextSnapshot fromHttpHeaders(Map<String, String> headers, String txnId) {
        String correlationId = get(headers, HDR_CORRELATION_ID);
        String workflowId    = get(headers, HDR_WORKFLOW_ID);
        String transactionId   = get(headers, HDR_TRANSACTION_ID);
        String principal     = get(headers, HDR_PRINCIPAL);
        String contextName   = get(headers, HDR_CONTEXT_NAME);
        

        boolean propagated =
        		transactionId != null ||
                correlationId != null ||
                workflowId    != null ||
                principal     != null ||
                contextName   != null;

        return ContextSnapshot.builder()
        		.transactionId(
                        transactionId != null ? transactionId : 
                        	(Context.transactionIdOrNull() != null ? Context.transactionIdOrNull() : txnId)
                )
                .correlationId(
                        correlationId != null ? correlationId : Context.correlationId()
                )
                .workflowId(
                        workflowId != null ? workflowId : Context.workflowIdOrNull()
                )
                .principal(
                        principal != null ? principal : Context.principalOrNull()
                )
                .contextName(
                        contextName != null ? contextName : Context.contextNameOrNull()
                )
                .propagated(propagated)
                .build();
    }

    // ---- Kafka ----

    public static ContextSnapshot fromKafkaHeaders(Function<String, byte[]> headerBytes) {
        String correlationId = utf8(headerBytes.apply("x-correlation-id"));
        String workflowId    = utf8(headerBytes.apply("x-workflow-id"));
        String principal     = utf8(headerBytes.apply("x-principal"));
        String contextName   = utf8(headerBytes.apply("x-context-name"));

        boolean propagated =
                correlationId != null ||
                workflowId    != null ||
                principal     != null ||
                contextName   != null;

        return ContextSnapshot.builder()
                .correlationId(
                        correlationId != null ? correlationId : Context.correlationId()
                )
                .workflowId(
                        workflowId != null ? workflowId : Context.workflowIdOrNull()
                )
                .principal(
                        principal != null ? principal : Context.principalOrNull()
                )
                .contextName(
                        contextName != null ? contextName : Context.contextNameOrNull()
                )
                .propagated(propagated)
                .build();
    }

    // ---- Simple / legacy ----

    public static ContextSnapshot fromCorrelationHeader(String headerValue) {
        boolean propagated = headerValue != null && !headerValue.isBlank();

        return ContextSnapshot.builder()
                .correlationId(
                        propagated ? headerValue.trim() : Context.correlationId()
                )
                .workflowId(Context.workflowIdOrNull())
                .principal(Context.principalOrNull())
                .contextName(Context.contextNameOrNull())
                .propagated(propagated)
                .build();
    }

    // ---- helpers ----

    private static String get(Map<String, String> headers, String key) {
        if (headers == null || key == null) return null;

        // case-insensitive HTTP headers
        for (Map.Entry<String, String> e : headers.entrySet()) {
            if (key.equalsIgnoreCase(e.getKey())) {
                String v = e.getValue();
                if (v != null && !v.isBlank()) return v.trim();
            }
        }
        return null;
    }

    private static String utf8(byte[] bytes) {
        if (bytes == null || bytes.length == 0) return null;
        String s = new String(bytes, StandardCharsets.UTF_8).trim();
        return s.isEmpty() ? null : s;
    }
}
