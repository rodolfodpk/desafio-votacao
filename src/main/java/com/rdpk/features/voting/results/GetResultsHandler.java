package com.rdpk.features.voting.results;

import com.rdpk.features.session.repository.VotingSessionRepository;
import com.rdpk.features.voting.domain.VotingResult;
import com.rdpk.features.voting.repository.VoteRepository;
import com.rdpk.exception.VotingException;
import com.rdpk.infrastructure.time.TimeProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class GetResultsHandler {

    private final VoteRepository voteRepository;
    private final VotingSessionRepository sessionRepository;
    private final TimeProvider timeProvider;

    public GetResultsHandler(VoteRepository voteRepository,
                            VotingSessionRepository sessionRepository,
                            TimeProvider timeProvider) {
        this.voteRepository = voteRepository;
        this.sessionRepository = sessionRepository;
        this.timeProvider = timeProvider;
    }

    public Mono<VotingResult> getResults(Long agendaId) {
        return sessionRepository.findByAgendaId(agendaId)
                .switchIfEmpty(Mono.error(new VotingException("Voting session not found", HttpStatus.NOT_FOUND)))
                .flatMap(session -> {
                    // Get vote counts from repository
                    return voteRepository.countVotesByAgendaId(agendaId)
                            .map(result -> {
                                // Determine status based on session end time
                                String status = timeProvider.now().isAfter(session.endTime()) ? "Closed" : "Open";
                                return new VotingResult(agendaId, result.yesVotes(), result.noVotes(), status);
                            });
                });
    }
}
