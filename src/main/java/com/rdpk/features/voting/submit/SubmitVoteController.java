package com.rdpk.features.voting.submit;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.github.resilience4j.reactor.ratelimiter.operator.RateLimiterOperator;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/api/agendas")
public class SubmitVoteController {

    private final SubmitVoteHandler submitVoteHandler;
    private final RateLimiter rateLimiter;

    public SubmitVoteController(
            SubmitVoteHandler submitVoteHandler,
            RateLimiterRegistry rateLimiterRegistry) {
        this.submitVoteHandler = submitVoteHandler;
        this.rateLimiter = rateLimiterRegistry.rateLimiter("voteSubmission");
    }

    @PostMapping("/{agendaId}/votes")
    public Mono<ResponseEntity<?>> submitVote(
            @PathVariable Long agendaId,
            @Valid @RequestBody SubmitVoteRequest request) {
        
        return Mono.defer(() -> {
            // Check CPF format after NotBlank validation
            if (!request.isValidCpfFormat()) {
                return Mono.just(ResponseEntity.badRequest()
                        .body(Map.of("error", "CPF must be 11 digits")));
            }
            
            return submitVoteHandler.submitVote(agendaId, request.cpf(), request.vote())
                    .map(vote -> ResponseEntity.status(HttpStatus.CREATED).body(vote));
        })
        .transform(RateLimiterOperator.of(rateLimiter))
        .onErrorResume(RequestNotPermitted.class, _ -> {
            return Mono.just(ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("error", "Too many requests. Please try again later.")));
        });
    }
}
