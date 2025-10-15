package com.rdpk.features.voting.repository;

import com.rdpk.features.voting.domain.Vote;
import com.rdpk.features.voting.domain.VotingResult;
import io.github.resilience4j.reactor.retry.RetryOperator;
import io.github.resilience4j.reactor.timelimiter.TimeLimiterOperator;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public class VoteRepositoryImpl implements VoteRepository {

    private static final Logger log = LoggerFactory.getLogger(VoteRepositoryImpl.class);

    private final R2dbcEntityTemplate template;
    private final DatabaseClient databaseClient;
    private final Retry retry;
    private final TimeLimiter timeLimiter;

    public VoteRepositoryImpl(
            R2dbcEntityTemplate template, 
            DatabaseClient databaseClient,
            RetryRegistry retryRegistry,
            TimeLimiterRegistry timeLimiterRegistry) {
        this.template = template;
        this.databaseClient = databaseClient;
        this.retry = retryRegistry.retry("database");
        this.timeLimiter = timeLimiterRegistry.timeLimiter("database");
    }

    @Override
    public Mono<Vote> save(Vote vote) {
        return Mono.defer(() -> {
            // Try to insert the vote directly
            // If it fails due to unique constraint, return the existing vote
            return template.insert(Vote.class)
                    .using(vote)
                    .onErrorResume(org.springframework.dao.DuplicateKeyException.class, _ -> {
                        // Vote already exists, return the existing vote
                        return template.selectOne(
                                Query.query(
                                        Criteria.where("agenda_id").is(vote.agendaId())
                                                .and("cpf").is(vote.cpf())
                                ),
                                Vote.class
                        );
                    });
        })
        // Apply database resilience
        .transform(RetryOperator.of(retry))
        .transform(TimeLimiterOperator.of(timeLimiter));
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
        return Mono.defer(() -> template.exists(
                Query.query(
                        Criteria.where("agenda_id").is(agendaId)
                                .and("cpf").is(cpf)
                ),
                Vote.class
        ))
        // Apply database resilience
        .transform(RetryOperator.of(retry))
        .transform(TimeLimiterOperator.of(timeLimiter));
    }
    
    @Override
    public Mono<VotingResult> countVotesByAgendaId(Long agendaId) {
        return Mono.defer(() -> {
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
        })
        // Apply database resilience
        .transform(RetryOperator.of(retry))
        .transform(TimeLimiterOperator.of(timeLimiter));
    }
}
