package com.rdpk.config;

import org.springframework.context.annotation.Configuration;

/**
 * Resilience4j configuration is handled by Spring Boot's auto-configuration.
 * Configuration is loaded from application properties files (application.properties, application-k6.properties, etc.).
 * 
 * The Resilience4j Spring Boot starter automatically creates and configures:
 * - CircuitBreakerRegistry
 * - RetryRegistry  
 * - TimeLimiterRegistry
 * - BulkheadRegistry
 * - RateLimiterRegistry
 * 
 * Based on the properties defined in the application properties files.
 */
@Configuration
public class ResilienceConfig {
    // Removed custom bean definitions to allow Spring Boot auto-configuration
    // to handle Resilience4j configuration from properties files
}
