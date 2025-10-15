package com.rdpk.e2e.config;

import com.rdpk.e2e.helpers.FixedTimeProvider;
import com.rdpk.infrastructure.time.TimeProvider;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.time.LocalDateTime;

@TestConfiguration
public class TimeProviderTestConfig {
    
    @Bean
    @Primary
    public TimeProvider timeProvider() {
        return new FixedTimeProvider(LocalDateTime.of(2025, 1, 1, 10, 0));
    }
}
