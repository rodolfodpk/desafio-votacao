package com.rdpk.features.agenda.create;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/agendas")
public class CreateAgendaController {

    private final CreateAgendaHandler createAgendaHandler;

    public CreateAgendaController(CreateAgendaHandler createAgendaHandler) {
        this.createAgendaHandler = createAgendaHandler;
    }

    @PostMapping
    public Mono<ResponseEntity<CreateAgendaResponse>> createAgenda(@Valid @RequestBody CreateAgendaRequest request) {
        return createAgendaHandler.createAgenda(request.title(), request.description())
                .map(CreateAgendaResponse::from)
                .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response));
    }
}
