package com.rdpk.features.voting.submit;

import com.rdpk.features.voting.domain.VoteChoice;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SubmitVoteRequest(
        @NotBlank(message = "CPF is required")
        String cpf,
        @NotNull(message = "Vote is required")
        VoteChoice vote
) {
    // Custom validation method to check CPF format after NotBlank validation
    public boolean isValidCpfFormat() {
        return cpf != null && cpf.matches("\\d{11}");
    }
}
