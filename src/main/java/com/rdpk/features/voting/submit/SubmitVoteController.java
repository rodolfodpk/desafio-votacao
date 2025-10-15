package com.rdpk.features.voting.submit;

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

    public SubmitVoteController(SubmitVoteHandler submitVoteHandler) {
        this.submitVoteHandler = submitVoteHandler;
    }

    @PostMapping("/{agendaId}/votes")
    public Mono<ResponseEntity<?>> submitVote(
            @PathVariable Long agendaId,
            @Valid @RequestBody SubmitVoteRequest request) {
        
        // Check CPF format after NotBlank validation
        if (!request.isValidCpfFormat()) {
            return Mono.just(ResponseEntity.badRequest()
                    .body(Map.of("error", "CPF must be 11 digits")));
        }
        
        return submitVoteHandler.submitVote(agendaId, request.cpf(), request.vote())
                .map(vote -> ResponseEntity.status(HttpStatus.CREATED).body(vote));
    }
}
