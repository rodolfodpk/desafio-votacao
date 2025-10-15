package com.rdpk.features.voting.repository;

import com.rdpk.features.voting.domain.Vote;
import com.rdpk.features.voting.domain.VoteChoice;
import com.rdpk.features.voting.domain.VotingResult;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Repository
public class VoteRepositoryImpl implements VoteRepository {

    private final ConcurrentHashMap<String, Vote> votes = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, AtomicInteger> yesVoteCounts = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, AtomicInteger> noVoteCounts = new ConcurrentHashMap<>();

    @Override
    public Mono<Vote> save(Vote vote) {
        String key = vote.agendaId() + ":" + vote.cpf();
        
        // Use putIfAbsent to ensure atomicity - only save if no vote exists
        Vote existingVote = votes.putIfAbsent(key, vote);
        
        if (existingVote != null) {
            // Vote already exists, return the existing vote
            return Mono.just(existingVote);
        }
        
        // Update running totals for high-volume performance
        if (vote.vote() == VoteChoice.YES) {
            yesVoteCounts.computeIfAbsent(vote.agendaId(), k -> new AtomicInteger()).incrementAndGet();
        } else {
            noVoteCounts.computeIfAbsent(vote.agendaId(), k -> new AtomicInteger()).incrementAndGet();
        }
        
        return Mono.just(vote);
    }

    @Override
    public Flux<Vote> findByAgendaId(Long agendaId) {
        return Flux.fromIterable(votes.values())
                .filter(vote -> vote.agendaId().equals(agendaId));
    }

    @Override
    public Mono<Boolean> existsByAgendaIdAndCpf(Long agendaId, String cpf) {
        String key = agendaId + ":" + cpf;
        return Mono.just(votes.containsKey(key));
    }

    // Method for test cleanup
    public void clear() {
        votes.clear();
        yesVoteCounts.clear();
        noVoteCounts.clear();
    }
    
    @Override
    public Mono<VotingResult> countVotesByAgendaId(Long agendaId) {
        // Use cached counts for O(1) performance
        int yesVotes = yesVoteCounts.getOrDefault(agendaId, new AtomicInteger()).get();
        int noVotes = noVoteCounts.getOrDefault(agendaId, new AtomicInteger()).get();
        
        // Determine status based on session (will be handled by service layer)
        String status = "Open"; // Default, service will determine actual status
        
        return Mono.just(new VotingResult(agendaId, yesVotes, noVotes, status));
    }
}
