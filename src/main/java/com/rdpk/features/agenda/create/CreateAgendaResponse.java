package com.rdpk.features.agenda.create;

import com.rdpk.features.agenda.domain.Agenda;
import java.time.LocalDateTime;

public record CreateAgendaResponse(
    Long id,
    String title,
    String description,
    LocalDateTime createdAt
) {
    public static CreateAgendaResponse from(Agenda agenda) {
        return new CreateAgendaResponse(
            agenda.id(),
            agenda.title(),
            agenda.description(),
            agenda.createdAt()
        );
    }
}

