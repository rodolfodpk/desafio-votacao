package com.rdpk.features.session.open;

import com.rdpk.features.agenda.repository.AgendaRepository;
import com.rdpk.features.session.domain.VotingSession;
import com.rdpk.features.session.repository.VotingSessionRepository;
import com.rdpk.exception.VotingException;
import com.rdpk.infrastructure.time.TimeProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class OpenSessionHandler {

    private final AgendaRepository agendaRepository;
    private final VotingSessionRepository sessionRepository;
    private final TimeProvider timeProvider;

    public OpenSessionHandler(AgendaRepository agendaRepository, 
                             VotingSessionRepository sessionRepository,
                             TimeProvider timeProvider) {
        this.agendaRepository = agendaRepository;
        this.sessionRepository = sessionRepository;
        this.timeProvider = timeProvider;
    }

    public Mono<VotingSession> openVotingSession(Long agendaId, Integer durationMinutes) {
        return agendaRepository.findById(agendaId)
                .switchIfEmpty(Mono.error(new VotingException("Agenda not found", HttpStatus.NOT_FOUND)))
                .flatMap(agenda -> {
                    // Check if session already exists
                    return sessionRepository.findByAgendaId(agendaId)
                            .flatMap(existing -> {
                                // Always return conflict if session exists (regardless of expiration)
                                return Mono.<VotingSession>error(new VotingException("Voting session already exists", HttpStatus.CONFLICT));
                            })
                            .switchIfEmpty(createNewSession(agendaId, durationMinutes));
                });
    }

    private Mono<VotingSession> createNewSession(Long agendaId, Integer durationMinutes) {
        int duration = durationMinutes != null ? durationMinutes : 1; // Default 1 minute
        VotingSession session = new VotingSession(agendaId, duration, timeProvider.now());
        return sessionRepository.save(session);
    }
}
