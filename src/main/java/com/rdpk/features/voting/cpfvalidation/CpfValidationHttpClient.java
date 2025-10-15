package com.rdpk.features.voting.cpfvalidation;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.reactor.bulkhead.operator.BulkheadOperator;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import io.github.resilience4j.reactor.retry.RetryOperator;
import io.github.resilience4j.reactor.timelimiter.TimeLimiterOperator;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class CpfValidationHttpClient implements CpfValidationService {

    private static final Logger log = LoggerFactory.getLogger(CpfValidationHttpClient.class);

    private final WebClient webClient;
    private final String baseUrl;
    private final CircuitBreaker circuitBreaker;
    private final Retry retry;
    private final TimeLimiter timeLimiter;
    private final Bulkhead bulkhead;

    public CpfValidationHttpClient(
            WebClient.Builder webClientBuilder,
            @Value("${cpf.validation.url:http://localhost:8080/api/cpf-validation}") String baseUrl,
            CircuitBreakerRegistry circuitBreakerRegistry,
            RetryRegistry retryRegistry,
            TimeLimiterRegistry timeLimiterRegistry,
            BulkheadRegistry bulkheadRegistry) {
        this.webClient = webClientBuilder.build();
        this.baseUrl = baseUrl;
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker("cpfValidation");
        this.retry = retryRegistry.retry("cpfValidation");
        this.timeLimiter = timeLimiterRegistry.timeLimiter("cpfValidation");
        this.bulkhead = bulkheadRegistry.bulkhead("cpfValidation");
        
        log.info("CPF Validation HTTP Client initialized with resilience patterns");
    }

    @Override
    public Mono<CpfValidationResponse> validateCpf(String cpf) {
        return Mono.defer(() -> webClient.get()
                .uri(baseUrl + "/{cpf}", cpf)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, response ->
                    Mono.error(new RuntimeException("CPF validation failed: " + response.statusCode())))
                .onStatus(HttpStatusCode::is5xxServerError, _ ->
                    Mono.error(new RuntimeException("CPF service unavailable")))
                .bodyToMono(CpfValidationResponse.class))
                // Apply resilience patterns in correct order
                .transform(BulkheadOperator.of(bulkhead))
                .transform(CircuitBreakerOperator.of(circuitBreaker))
                .transform(RetryOperator.of(retry))
                .transform(TimeLimiterOperator.of(timeLimiter))
                // Fallback: If all resilience patterns fail, return safe default
                .onErrorResume(throwable -> {
                    log.warn("CPF validation failed for {}: {}. Returning UNABLE_TO_VOTE fallback", 
                            cpf, throwable.getMessage());
                    return Mono.just(new CpfValidationResponse("UNABLE_TO_VOTE"));
                });
    }
}
