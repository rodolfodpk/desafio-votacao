package com.rdpk.features.voting.domain;

import java.time.LocalDateTime;

public record Vote(
        Long agendaId,
        String cpf,
        VoteChoice vote,
        LocalDateTime votedAt
) {
    public Vote(Long agendaId, String cpf, VoteChoice vote) {
        this(agendaId, cpf, vote, LocalDateTime.now());
    }
}
