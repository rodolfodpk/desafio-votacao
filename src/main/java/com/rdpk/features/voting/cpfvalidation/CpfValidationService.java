package com.rdpk.features.voting.cpfvalidation;

import reactor.core.publisher.Mono;

public interface CpfValidationService {
    Mono<CpfValidationResponse> validateCpf(String cpf);
}
