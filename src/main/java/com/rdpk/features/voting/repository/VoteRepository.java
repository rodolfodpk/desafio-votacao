package com.rdpk.features.voting.repository;

import com.rdpk.features.voting.domain.Vote;
import com.rdpk.features.voting.domain.VotingResult;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface VoteRepository {
    Mono<Vote> save(Vote vote);
    Flux<Vote> findByAgendaId(Long agendaId);
    Mono<Boolean> existsByAgendaIdAndCpf(Long agendaId, String cpf);
    Mono<VotingResult> countVotesByAgendaId(Long agendaId);
}
