/**
 * VerityNow context library.
 *
 * Design goals:
 * - Autonomous (no framework required)
 * - Stable, semantic API for correlation/transaction/principal
 * - Optional MDC mirroring via reflection if SLF4J is present
 * - Boundary helpers in ContextResolvers without carrier-type coupling
 */
package com.veritynow.context;
