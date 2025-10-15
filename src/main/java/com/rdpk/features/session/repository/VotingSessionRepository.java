package com.rdpk.features.session.repository;

import com.rdpk.features.session.domain.VotingSession;
import reactor.core.publisher.Mono;

public interface VotingSessionRepository {
    Mono<VotingSession> save(VotingSession session);
    Mono<VotingSession> findByAgendaId(Long agendaId);
}
