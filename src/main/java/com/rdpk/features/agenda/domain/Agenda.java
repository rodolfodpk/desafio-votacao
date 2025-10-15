package com.rdpk.features.agenda.domain;

import java.time.LocalDateTime;

public record Agenda(
        Long id,
        String title,
        String description,
        LocalDateTime createdAt
) {
    public Agenda(String title, String description) {
        this(null, title, description, LocalDateTime.now());
    }
}
