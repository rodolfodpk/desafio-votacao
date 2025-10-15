package com.rdpk.features.voting.submit;

import com.rdpk.features.session.repository.VotingSessionRepository;
import com.rdpk.features.voting.domain.Vote;
import com.rdpk.features.voting.domain.VoteChoice;
import com.rdpk.features.voting.repository.VoteRepository;
import com.rdpk.exception.VotingException;
import com.rdpk.features.voting.cpfvalidation.CpfValidationService;
import com.rdpk.infrastructure.time.TimeProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class SubmitVoteHandler {

    private final VoteRepository voteRepository;
    private final VotingSessionRepository sessionRepository;
    private final CpfValidationService cpfValidationService;
    private final TimeProvider timeProvider;

    public SubmitVoteHandler(VoteRepository voteRepository,
                            VotingSessionRepository sessionRepository,
                            CpfValidationService cpfValidationService,
                            TimeProvider timeProvider) {
        this.voteRepository = voteRepository;
        this.sessionRepository = sessionRepository;
        this.cpfValidationService = cpfValidationService;
        this.timeProvider = timeProvider;
    }

    public Mono<Vote> submitVote(Long agendaId, String cpf, VoteChoice vote) {
        // First validate CPF
        return cpfValidationService.validateCpf(cpf)
                .flatMap(response -> {
                    if ("UNABLE_TO_VOTE".equals(response.status())) {
                        return Mono.error(new VotingException("CPF is not able to vote", HttpStatus.NOT_FOUND));
                    }
                    return validateAndSubmitVote(agendaId, cpf, vote);
                })
                .onErrorResume(throwable -> {
                    // Handle CPF validation errors
                    if (throwable.getMessage() != null && throwable.getMessage().contains("CPF validation failed")) {
                        return Mono.error(new VotingException("CPF is not able to vote", HttpStatus.NOT_FOUND));
                    }
                    // Re-throw other exceptions
                    return Mono.error(throwable);
                });
    }

    private Mono<Vote> validateAndSubmitVote(Long agendaId, String cpf, VoteChoice vote) {
        return sessionRepository.findByAgendaId(agendaId)
                .switchIfEmpty(Mono.error(new VotingException("Voting session not found", HttpStatus.NOT_FOUND)))
                .flatMap(session -> {
                    // Check if session is still open
                    if (timeProvider.now().isAfter(session.endTime())) {
                        return Mono.error(new VotingException("Voting session is closed", HttpStatus.BAD_REQUEST));
                    }
                    
                    // Try to save the vote - the repository will handle duplicates atomically
                    return saveVote(agendaId, cpf, vote);
                });
    }

    private Mono<Vote> saveVote(Long agendaId, String cpf, VoteChoice vote) {
        Vote newVote = new Vote(agendaId, cpf, vote);
        
        // Check if vote already exists before attempting to save
        return voteRepository.existsByAgendaIdAndCpf(agendaId, cpf)
                .flatMap(exists -> {
                    if (exists) {
                        return Mono.error(new VotingException("CPF already voted for this agenda", HttpStatus.BAD_REQUEST));
                    }
                    
                    // Vote doesn't exist, proceed with save
                    return voteRepository.save(newVote);
                })
                .onErrorResume(org.springframework.dao.DuplicateKeyException.class, _ -> 
                    Mono.error(new VotingException("CPF already voted for this agenda", HttpStatus.BAD_REQUEST))
                );
    }
}
