package com.rdpk.features.voting.results;

import com.rdpk.features.voting.domain.VotingResult;

public record GetResultsResponse(
    Long agendaId,
    int yesVotes,
    int noVotes,
    String status
) {
    public static GetResultsResponse from(VotingResult result) {
        return new GetResultsResponse(
            result.agendaId(),
            result.yesVotes(),
            result.noVotes(),
            result.status()
        );
    }
}

