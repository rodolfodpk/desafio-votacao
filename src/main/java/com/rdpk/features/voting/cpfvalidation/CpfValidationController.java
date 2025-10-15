package com.rdpk.features.voting.cpfvalidation;

import br.com.caelum.stella.validation.CPFValidator;
import com.rdpk.config.CpfValidationConfig;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/cpf-validation")
public class CpfValidationController {

    private final CPFValidator cpfValidator = new CPFValidator();
    private final CpfValidationConfig config;
    
    public CpfValidationController(CpfValidationConfig config) {
        this.config = config;
    }

    @GetMapping("/{cpf}")
    public Mono<ResponseEntity<CpfValidationResponse>> validateCpf(@PathVariable String cpf) {
        // If lenient mode is enabled, accept any CPF
        if (config.isLenient()) {
            return Mono.just(ResponseEntity.ok(
                new CpfValidationResponse("ABLE_TO_VOTE")
            ));
        }
        
        // Otherwise, use strict validation with caelum-stella
        if (isValidRealCpf(cpf)) {
            return Mono.just(ResponseEntity.ok(
                new CpfValidationResponse("ABLE_TO_VOTE")
            ));
        } else {
            return Mono.just(ResponseEntity.notFound().build());
        }
    }
    
    // Helper method to validate CPF using caelum-stella
    private boolean isValidRealCpf(String cpf) {
        try {
            return cpfValidator.isEligible(cpf) && cpfValidator.invalidMessagesFor(cpf).isEmpty();
        } catch (Exception e) {
            return false;
        }
    }
}
