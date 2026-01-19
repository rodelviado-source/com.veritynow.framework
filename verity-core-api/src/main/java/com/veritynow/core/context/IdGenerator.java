package com.veritynow.core.context;

/**
 * Correlation id generator.
 * Default uses UUID v4 string.
 */
public interface IdGenerator {
    String newCorrelationId();
}
