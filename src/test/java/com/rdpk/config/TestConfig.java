package com.rdpk.config;

import br.com.caelum.stella.validation.CPFValidator;
import com.rdpk.features.voting.cpfvalidation.CpfValidationResponse;
import com.rdpk.features.voting.cpfvalidation.CpfValidationService;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import reactor.core.publisher.Mono;

@TestConfiguration
public class TestConfig {

    @Bean
    @Primary
    @Profile("!strict")
    public CpfValidationService lenientMockCpfValidationService() {
        return new CpfValidationService() {
            @Override
            public Mono<CpfValidationResponse> validateCpf(String cpf) {
                // Mock implementation for lenient tests - accept all CPFs
                // In lenient mode, all CPFs should be accepted regardless of format
                return Mono.just(new CpfValidationResponse("ABLE_TO_VOTE"));
            }
        };
    }

    @Bean
    @Primary
    @Profile("strict")
    public CpfValidationService strictMockCpfValidationService() {
        return new CpfValidationService() {
            private final CPFValidator cpfValidator = new CPFValidator();
            
            @Override
            public Mono<CpfValidationResponse> validateCpf(String cpf) {
                // Strict validation using caelum-stella for CPF validation tests
                try {
                    if (cpfValidator.isEligible(cpf) && cpfValidator.invalidMessagesFor(cpf).isEmpty()) {
                        return Mono.just(new CpfValidationResponse("ABLE_TO_VOTE"));
                    } else {
                        return Mono.error(new RuntimeException("CPF validation failed"));
                    }
                } catch (Exception e) {
                    return Mono.error(new RuntimeException("CPF validation failed"));
                }
            }
        };
    }
}
