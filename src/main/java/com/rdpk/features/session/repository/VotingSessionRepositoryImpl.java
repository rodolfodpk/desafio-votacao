package com.rdpk.features.session.repository;

import com.rdpk.features.session.domain.VotingSession;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public class VotingSessionRepositoryImpl implements VotingSessionRepository {

    private final R2dbcEntityTemplate template;

    public VotingSessionRepositoryImpl(R2dbcEntityTemplate template) {
        this.template = template;
    }

    @Override
    public Mono<VotingSession> save(VotingSession session) {
        if (session.sessionId() == null) {
            return template.insert(VotingSession.class)
                    .using(session);
        } else {
            return template.update(session);
        }
    }

    @Override
    public Mono<VotingSession> findByAgendaId(Long agendaId) {
        return template.selectOne(
                Query.query(Criteria.where("agenda_id").is(agendaId)),
                VotingSession.class
        );
    }
}
