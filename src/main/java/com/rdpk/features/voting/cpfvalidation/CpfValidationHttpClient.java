package com.rdpk.features.voting.cpfvalidation;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class CpfValidationHttpClient implements CpfValidationService {

    private final WebClient webClient;
    private final String baseUrl;

    public CpfValidationHttpClient(WebClient.Builder webClientBuilder, 
                                  @Value("${cpf.validation.url:http://localhost:8080/api/cpf-validation}") String baseUrl) {
        this.webClient = webClientBuilder.build();
        this.baseUrl = baseUrl;
    }

    @Override
    public Mono<CpfValidationResponse> validateCpf(String cpf) {
        return webClient.get()
                .uri(baseUrl + "/{cpf}", cpf)
                .retrieve()
                .onStatus(status -> status.is4xxClientError(), response -> 
                    Mono.just(new RuntimeException("CPF validation failed")))
                .bodyToMono(CpfValidationResponse.class)
                .onErrorReturn(new CpfValidationResponse("UNABLE_TO_VOTE"));
    }
}
