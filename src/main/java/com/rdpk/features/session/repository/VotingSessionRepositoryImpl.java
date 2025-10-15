package com.rdpk.features.session.repository;

import com.rdpk.features.session.domain.VotingSession;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class VotingSessionRepositoryImpl implements VotingSessionRepository {

    private final ConcurrentHashMap<Long, VotingSession> sessions = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    @Override
    public Mono<VotingSession> save(VotingSession session) {
        if (session.sessionId() == null) {
            Long newId = idGenerator.getAndIncrement();
            VotingSession newSession = new VotingSession(newId, session.agendaId(), 
                session.durationMinutes(), session.endTime());
            sessions.put(session.agendaId(), newSession);
            return Mono.just(newSession);
        } else {
            sessions.put(session.agendaId(), session);
            return Mono.just(session);
        }
    }

    @Override
    public Mono<VotingSession> findByAgendaId(Long agendaId) {
        return Mono.justOrEmpty(sessions.get(agendaId));
    }
    
    // Method for test cleanup
    public void clear() {
        sessions.clear();
        idGenerator.set(1);
    }
}
