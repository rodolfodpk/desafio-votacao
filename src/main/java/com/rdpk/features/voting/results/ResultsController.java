package com.rdpk.features.voting.results;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/agendas")
public class ResultsController {

    private final GetResultsHandler getResultsHandler;

    public ResultsController(GetResultsHandler getResultsHandler) {
        this.getResultsHandler = getResultsHandler;
    }

    @GetMapping("/{agendaId}/results")
    public Mono<ResponseEntity<GetResultsResponse>> getResults(@PathVariable Long agendaId) {
        return getResultsHandler.getResults(agendaId)
                .map(GetResultsResponse::from)
                .map(ResponseEntity::ok);
    }
}
