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
        return Mono.defer(() -> template.insert(Vote.class).using(vote))
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
        return Mono.defer(() -> databaseClient.sql("""
                SELECT 
                    COALESCE(SUM(CASE WHEN vote_value = 'YES' THEN 1 ELSE 0 END), 0) as yes_votes,
                    COALESCE(SUM(CASE WHEN vote_value = 'NO' THEN 1 ELSE 0 END), 0) as no_votes
                FROM votes
                WHERE agenda_id = :agendaId
                """)
                .bind("agendaId", agendaId)
                .fetch()
                .one()
                .map(row -> new VotingResult(
                        agendaId,
                        ((Number) row.get("yes_votes")).intValue(),
                        ((Number) row.get("no_votes")).intValue(),
                        "Open" // Default status, will be determined by service layer
                )))
        // Apply database resilience
        .transform(RetryOperator.of(retry))
        .transform(TimeLimiterOperator.of(timeLimiter));
    }
}
