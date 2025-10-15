package com.rdpk.features.session.open;

import com.rdpk.features.session.domain.VotingSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/agendas")
public class OpenSessionController {

    private final OpenSessionHandler openSessionHandler;

    public OpenSessionController(OpenSessionHandler openSessionHandler) {
        this.openSessionHandler = openSessionHandler;
    }

    @PostMapping("/{agendaId}/voting-session")
    public Mono<ResponseEntity<VotingSession>> openVotingSession(
            @PathVariable Long agendaId,
            @RequestBody(required = false) OpenSessionRequest request) {
        
        Integer durationMinutes = request != null ? request.durationMinutes() : null;
        return openSessionHandler.openVotingSession(agendaId, durationMinutes)
                .map(session -> ResponseEntity.status(HttpStatus.CREATED).body(session));
    }
}
