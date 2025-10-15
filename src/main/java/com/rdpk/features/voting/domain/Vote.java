package com.rdpk.features.voting.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table("votes")
public record Vote(
        @Id
        Long id,
        @Column("agenda_id")
        Long agendaId,
        String cpf,
        @Column("vote_value")
        VoteChoice vote,
        @Column("voted_at")
        LocalDateTime votedAt
) {
    public Vote(Long agendaId, String cpf, VoteChoice vote) {
        this(null, agendaId, cpf, vote, LocalDateTime.now());
    }
}
