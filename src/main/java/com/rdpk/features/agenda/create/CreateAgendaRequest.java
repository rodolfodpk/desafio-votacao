package com.rdpk.features.agenda.create;

import jakarta.validation.constraints.NotBlank;

public record CreateAgendaRequest(
        @NotBlank(message = "Title is required")
        String title,
        String description
) {
}
