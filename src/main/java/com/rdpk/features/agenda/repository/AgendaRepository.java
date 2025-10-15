package com.rdpk.features.agenda.repository;

import com.rdpk.features.agenda.domain.Agenda;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface AgendaRepository {
    Mono<Agenda> save(Agenda agenda);
    Mono<Agenda> findById(Long id);
    Flux<Agenda> findAll();
}
