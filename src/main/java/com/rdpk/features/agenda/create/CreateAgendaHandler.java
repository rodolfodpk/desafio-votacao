package com.rdpk.features.agenda.create;

import com.rdpk.features.agenda.domain.Agenda;
import com.rdpk.features.agenda.repository.AgendaRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class CreateAgendaHandler {

    private final AgendaRepository agendaRepository;

    public CreateAgendaHandler(AgendaRepository agendaRepository) {
        this.agendaRepository = agendaRepository;
    }

    public Mono<Agenda> createAgenda(String title, String description) {
        Agenda agenda = new Agenda(title, description);
        return agendaRepository.save(agenda);
    }
}
