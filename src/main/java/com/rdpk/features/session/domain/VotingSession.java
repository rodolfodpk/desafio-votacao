package com.rdpk.features.session.domain;

import java.time.LocalDateTime;

public record VotingSession(
        Long sessionId,
        Long agendaId,
        Integer durationMinutes,
        LocalDateTime endTime
) {
    public VotingSession(Long agendaId, Integer durationMinutes, LocalDateTime currentTime) {
        this(null, agendaId, durationMinutes, currentTime.plusMinutes(durationMinutes));
    }
}
