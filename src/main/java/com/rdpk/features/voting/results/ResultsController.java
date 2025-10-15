package com.rdpk.features.voting.results;

import com.rdpk.features.voting.domain.VotingResult;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/agendas")
public class ResultsController {

    private final GetResultsHandler getResultsHandler;

    public ResultsController(GetResultsHandler getResultsHandler) {
        this.getResultsHandler = getResultsHandler;
    }

    @GetMapping("/{agendaId}/results")
    public Mono<ResponseEntity<VotingResult>> getResults(@PathVariable Long agendaId) {
        return getResultsHandler.getResults(agendaId)
                .map(ResponseEntity::ok);
    }
}
