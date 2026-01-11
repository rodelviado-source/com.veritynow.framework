package com.veritynow.context;

/**
 * Utilities for building ContextSnapshot at system boundaries (REST, messaging, etc.)
 * without coupling the core context library to carrier types (Servlet/Kafka/etc).
 */
public final class ContextResolvers {

    private ContextResolvers() {}

    /**
     * Build a snapshot from a correlation header value (e.g., X-Correlation-Id).
     * If header is blank/missing, uses Context.correlationId() which will generate one if absent.
     */
    public static ContextSnapshot fromCorrelationHeader(String headerValue) {
        boolean propagated = headerValue != null && !headerValue.isBlank();
        String cid = propagated ? headerValue.trim() : Context.correlationId();

        return ContextSnapshot.builder()
                .correlationId(cid)
                .propagated(propagated)
                .build();
    }
}
