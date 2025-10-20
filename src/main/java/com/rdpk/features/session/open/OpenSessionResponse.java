package com.rdpk.features.session.open;

import com.rdpk.features.session.domain.VotingSession;
import java.time.LocalDateTime;

public record OpenSessionResponse(
    Long id,
    Long agendaId,
    Integer durationMinutes,
    LocalDateTime endTime
) {
    public static OpenSessionResponse from(VotingSession session) {
        return new OpenSessionResponse(
            session.sessionId(),
            session.agendaId(),
            session.durationMinutes(),
            session.endTime()
        );
    }
}

