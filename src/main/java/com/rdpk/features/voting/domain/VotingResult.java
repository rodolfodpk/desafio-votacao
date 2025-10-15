package com.rdpk.features.voting.domain;

public record VotingResult(
        Long agendaId,
        Integer yesVotes,
        Integer noVotes,
        String status
) {
}
