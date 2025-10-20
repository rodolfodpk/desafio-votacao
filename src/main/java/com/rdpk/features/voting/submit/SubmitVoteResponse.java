package com.rdpk.features.voting.submit;

import com.rdpk.features.voting.domain.Vote;
import java.time.LocalDateTime;

public record SubmitVoteResponse(
    Long id,
    Long agendaId,
    String cpf,
    String vote,
    LocalDateTime votedAt
) {
    public static SubmitVoteResponse from(Vote vote) {
        // Format vote as "Yes" or "No" (capitalize first letter, lowercase rest)
        String voteString = vote.vote().name();
        String formattedVote = voteString.charAt(0) + voteString.substring(1).toLowerCase();
        
        return new SubmitVoteResponse(
            vote.id(),
            vote.agendaId(),
            vote.cpf(),
            formattedVote,
            vote.votedAt()
        );
    }
}

