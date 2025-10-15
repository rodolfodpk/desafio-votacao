package com.rdpk.features.session.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table("voting_sessions")
public record VotingSession(
        @Id
        @Column("id")
        Long sessionId,
        @Column("agenda_id")
        Long agendaId,
        @Column("duration_minutes")
        Integer durationMinutes,
        @Column("end_time")
        LocalDateTime endTime
) {
    public VotingSession(Long agendaId, Integer durationMinutes, LocalDateTime currentTime) {
        this(null, agendaId, durationMinutes, currentTime.plusMinutes(durationMinutes));
    }
}
