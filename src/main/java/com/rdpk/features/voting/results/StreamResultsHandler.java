package com.rdpk.features.voting.results;

import com.rdpk.features.voting.domain.Vote;
import com.rdpk.features.voting.domain.VotingResult;
import com.rdpk.features.voting.repository.VoteRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.util.concurrent.ConcurrentHashMap;

@Service
public class StreamResultsHandler {

    private final GetResultsHandler getResultsHandler;
    private final VoteRepository voteRepository;
    
    // Real-time event streaming for SSE
    private final ConcurrentHashMap<Long, Sinks.Many<Vote>> voteSinks = new ConcurrentHashMap<>();

    public StreamResultsHandler(GetResultsHandler getResultsHandler,
                               VoteRepository voteRepository) {
        this.getResultsHandler = getResultsHandler;
        this.voteRepository = voteRepository;
    }

    public Flux<VotingResult> streamResults(Long agendaId) {
        // Send initial current results
        Mono<VotingResult> initial = getResultsHandler.getResults(agendaId);
        
        // Stream updates whenever new vote comes in
        Flux<VotingResult> updates = getSinkForAgenda(agendaId)
                .asFlux()
                .flatMap(vote -> getResultsHandler.getResults(agendaId)); // Recompute after each vote
        
        // Combine: send initial result, then updates
        return Flux.concat(initial, updates)
                .takeUntil(result -> "Closed".equals(result.status())); // Stop when closed
    }

    private Sinks.Many<Vote> getSinkForAgenda(Long agendaId) {
        return voteSinks.computeIfAbsent(agendaId, 
            id -> Sinks.many().multicast().onBackpressureBuffer());
    }

    // Method to publish vote events (called by SubmitVoteHandler)
    public void publishVoteEvent(Vote vote) {
        getSinkForAgenda(vote.agendaId()).tryEmitNext(vote);
    }
}
