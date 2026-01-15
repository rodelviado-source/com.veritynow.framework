package com.veritynow.v2.txn.adapters.spring;


import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Manual configuration entry point for non-Spring-Boot usage.
 *
 * In Spring Boot applications, {@link TxnAutoConfiguration} is loaded automatically via
 * META-INF auto-configuration imports. In plain Spring contexts, users may {@code @Import}
 * this configuration to enable the same turn-key defaults.
 */
@Configuration(proxyBeanMethods = false)
@Import(TxnAutoConfiguration.class)
public class TxnAutoConfigurationImport {
}
