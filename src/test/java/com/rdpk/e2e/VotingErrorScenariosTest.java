package com.rdpk.e2e;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.MediaType;

import static com.rdpk.e2e.helpers.VoteTestData.*;

/**
 * Tests various error scenarios in the voting system.
 * Uses default lenient profile for CPF validation.
 */
class VotingErrorScenariosTest extends AbstractE2eTest {

    private Long agendaId;

    @BeforeEach
    void setUp() {
        super.setUp();
        
        // Create a fresh agenda for each test
        agendaId = createTestAgenda("Error Test Agenda", "Test Description");
    }

    @ParameterizedTest
    @MethodSource("com.rdpk.e2e.helpers.VoteTestData#invalidCpfProvider")
    void testInvalidCpfFormats(String cpf, String expectedError) {
        // Create session first
        String sessionJson = createSessionJson(2);
        client.post()
                .uri("/api/v1/agendas/{agendaId}/voting-session", agendaId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(sessionJson)
                .exchange()
                .expectStatus().isCreated();

        // Try to vote with invalid CPF
        String voteJson = createVoteJson(cpf, "Yes");
        
        client.post()
                .uri("/api/v1/agendas/{agendaId}/votes", agendaId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(voteJson)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.error").isEqualTo(expectedError);
    }

    @ParameterizedTest
    @MethodSource("com.rdpk.e2e.helpers.VoteTestData#duplicateVoteProvider")
    void testDuplicateVotePrevention(String cpf, String firstVote, String secondVote) {
        // Create session first
        String sessionJson = createSessionJson(2);
        client.post()
                .uri("/api/v1/agendas/{agendaId}/voting-session", agendaId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(sessionJson)
                .exchange()
                .expectStatus().isCreated();

        // Submit first vote
        String firstVoteJson = createVoteJson(cpf, firstVote);
        client.post()
                .uri("/api/v1/agendas/{agendaId}/votes", agendaId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(firstVoteJson)
                .exchange()
                .expectStatus().isCreated();

        // Try to submit duplicate vote (same CPF)
        String secondVoteJson = createVoteJson(cpf, secondVote);
        client.post()
                .uri("/api/v1/agendas/{agendaId}/votes", agendaId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(secondVoteJson)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.error").isEqualTo("CPF already voted for this agenda");
    }

    @Test
    void testVoteWithoutSession() {
        // Try to vote without creating a session first
        String voteJson = createVoteJson(getKnownValidCpf(), "Yes");
        
        client.post()
                .uri("/api/v1/agendas/{agendaId}/votes", agendaId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(voteJson)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.error").isEqualTo("Voting session not found");
    }

    @Test
    void testOpenSessionTwice() {
        // Create first session
        String sessionJson = createSessionJson(2);
        client.post()
                .uri("/api/v1/agendas/{agendaId}/voting-session", agendaId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(sessionJson)
                .exchange()
                .expectStatus().isCreated();

        // Try to create second session for same agenda
        client.post()
                .uri("/api/v1/agendas/{agendaId}/voting-session", agendaId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(sessionJson)
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectBody()
                .jsonPath("$.error").isEqualTo("Voting session already exists");
    }

    @Test
    void testVoteWithInvalidVoteValue() {
        // Create session first
        String sessionJson = createSessionJson(2);
        client.post()
                .uri("/api/v1/agendas/{agendaId}/voting-session", agendaId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(sessionJson)
                .exchange()
                .expectStatus().isCreated();

        // Try to vote with invalid vote value
        String invalidVoteJson = """
                {
                    "cpf": "%s",
                    "vote": "Maybe"
                }
                """.formatted(getKnownValidCpf());
        
        client.post()
                .uri("/api/v1/agendas/{agendaId}/votes", agendaId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(invalidVoteJson)
                .exchange()
                .expectStatus().isEqualTo(500); // Jackson deserialization error
    }

    @Test
    void testVoteWithMissingCpf() {
        // Create session first
        String sessionJson = createSessionJson(2);
        client.post()
                .uri("/api/v1/agendas/{agendaId}/voting-session", agendaId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(sessionJson)
                .exchange()
                .expectStatus().isCreated();

        // Try to vote with missing CPF (null value)
        String missingCpfJson = """
                {
                    "cpf": null,
                    "vote": "Yes"
                }
                """;
        
        client.post()
                .uri("/api/v1/agendas/{agendaId}/votes", agendaId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(missingCpfJson)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.error").isEqualTo("CPF is required");
    }

    @Test
    void testVoteWithMissingVote() {
        // Create session first
        String sessionJson = createSessionJson(2);
        client.post()
                .uri("/api/v1/agendas/{agendaId}/voting-session", agendaId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(sessionJson)
                .exchange()
                .expectStatus().isCreated();

        // Try to vote with missing vote (null value)
        String missingVoteJson = """
                {
                    "cpf": "%s",
                    "vote": null
                }
                """.formatted(getKnownValidCpf());
        
        client.post()
                .uri("/api/v1/agendas/{agendaId}/votes", agendaId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(missingVoteJson)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.error").isEqualTo("Vote is required");
    }

    @Test
    void testGetResultsWithoutSession() {
        // Try to get results without creating a session
        client.get()
                .uri("/api/v1/agendas/{agendaId}/results", agendaId)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.error").isEqualTo("Voting session not found");
    }

    @Test
    void testVoteOnNonExistentAgenda() {
        Long nonExistentAgendaId = 999L;
        
        // Create session for non-existent agenda
        String sessionJson = createSessionJson(2);
        client.post()
                .uri("/api/v1/agendas/{agendaId}/voting-session", nonExistentAgendaId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(sessionJson)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.error").isEqualTo("Agenda not found");
    }
}
