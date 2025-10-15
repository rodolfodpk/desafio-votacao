package com.rdpk.features.voting.repository;

import com.rdpk.features.voting.domain.Vote;
import com.rdpk.features.voting.domain.VoteChoice;
import com.rdpk.features.voting.domain.VotingResult;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public class VoteRepositoryImpl implements VoteRepository {

    private final R2dbcEntityTemplate template;
    private final DatabaseClient databaseClient;

    public VoteRepositoryImpl(R2dbcEntityTemplate template, DatabaseClient databaseClient) {
        this.template = template;
        this.databaseClient = databaseClient;
    }

    @Override
    public Mono<Vote> save(Vote vote) {
        // Try to insert the vote directly
        // If it fails due to unique constraint, return the existing vote
        return template.insert(Vote.class)
                .using(vote)
                .onErrorResume(org.springframework.dao.DuplicateKeyException.class, ex -> {
                    // Vote already exists, return the existing vote
                    return template.selectOne(
                            Query.query(
                                    Criteria.where("agenda_id").is(vote.agendaId())
                                            .and("cpf").is(vote.cpf())
                            ),
                            Vote.class
                    );
                });
    }

    @Override
    public Flux<Vote> findByAgendaId(Long agendaId) {
        return template.select(
                Query.query(Criteria.where("agenda_id").is(agendaId)),
                Vote.class
        );
    }

    @Override
    public Mono<Boolean> existsByAgendaIdAndCpf(Long agendaId, String cpf) {
        return template.exists(
                Query.query(
                        Criteria.where("agenda_id").is(agendaId)
                                .and("cpf").is(cpf)
                ),
                Vote.class
        );
    }
    
    @Override
    public Mono<VotingResult> countVotesByAgendaId(Long agendaId) {
        // Count YES votes
        Mono<Long> yesCount = template.count(
                Query.query(
                        Criteria.where("agenda_id").is(agendaId)
                                .and("vote_value").is("YES")
                ),
                Vote.class
        );
        
        // Count NO votes
        Mono<Long> noCount = template.count(
                Query.query(
                        Criteria.where("agenda_id").is(agendaId)
                                .and("vote_value").is("NO")
                ),
                Vote.class
        );
        
        // Combine counts
        return Mono.zip(yesCount, noCount)
                .map(tuple -> new VotingResult(
                        agendaId,
                        tuple.getT1().intValue(),
                        tuple.getT2().intValue(),
                        "Open" // Default status, will be determined by service layer
                ));
    }
}
