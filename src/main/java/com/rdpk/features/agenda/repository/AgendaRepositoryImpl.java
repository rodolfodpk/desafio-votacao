package com.rdpk.features.agenda.repository;

import com.rdpk.features.agenda.domain.Agenda;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class AgendaRepositoryImpl implements AgendaRepository {

    private final ConcurrentHashMap<Long, Agenda> agendas = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    @Override
    public Mono<Agenda> save(Agenda agenda) {
        if (agenda.id() == null) {
            Long newId = idGenerator.getAndIncrement();
            Agenda newAgenda = new Agenda(newId, agenda.title(), agenda.description(), agenda.createdAt());
            agendas.put(newId, newAgenda);
            return Mono.just(newAgenda);
        } else {
            agendas.put(agenda.id(), agenda);
            return Mono.just(agenda);
        }
    }

    @Override
    public Mono<Agenda> findById(Long id) {
        return Mono.justOrEmpty(agendas.get(id));
    }

    @Override
    public Flux<Agenda> findAll() {
        return Flux.fromIterable(agendas.values());
    }
    
    // Method for test cleanup
    public void clear() {
        agendas.clear();
        idGenerator.set(1);
    }
}
