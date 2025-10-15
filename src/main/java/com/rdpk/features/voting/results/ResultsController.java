package com.rdpk.features.voting.results;

import com.rdpk.features.voting.domain.VotingResult;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/agendas")
public class ResultsController {

    private final GetResultsHandler getResultsHandler;
    private final StreamResultsHandler streamResultsHandler;

    public ResultsController(GetResultsHandler getResultsHandler,
                            StreamResultsHandler streamResultsHandler) {
        this.getResultsHandler = getResultsHandler;
        this.streamResultsHandler = streamResultsHandler;
    }

    @GetMapping("/{agendaId}/results")
    public Mono<ResponseEntity<VotingResult>> getResults(@PathVariable Long agendaId) {
        return getResultsHandler.getResults(agendaId)
                .map(ResponseEntity::ok);
    }

    @GetMapping(value = "/{agendaId}/results/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<VotingResult> streamResults(@PathVariable Long agendaId) {
        return streamResultsHandler.streamResults(agendaId);
    }
}
