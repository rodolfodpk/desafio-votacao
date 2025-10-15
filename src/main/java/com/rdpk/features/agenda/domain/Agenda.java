package com.rdpk.features.agenda.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table("agendas")
public record Agenda(
        @Id
        Long id,
        String title,
        String description,
        @Column("created_at")
        LocalDateTime createdAt
) {
    public Agenda(String title, String description) {
        this(null, title, description, LocalDateTime.now());
    }
}
