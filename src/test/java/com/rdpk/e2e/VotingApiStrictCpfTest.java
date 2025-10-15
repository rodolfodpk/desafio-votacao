package com.rdpk.e2e;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;

/**
 * Tests CPF validation with strict mode enabled.
 * Uses "strict" profile for real CPF validation.
 */
@ActiveProfiles("strict")
class VotingApiStrictCpfTest extends AbstractE2eTest {

    private Long testAgendaId;

    @BeforeEach
    void setUp() {
        super.setUp();
        
        // Create a fresh agenda for each test
        testAgendaId = createTestAgenda("Test Agenda - Strict CPF", "Test Description for Strict CPF Validation");
    }

    @Test
    void testVotingWithRealValidCpf() {
        // Agenda is already created in setUp()

        // 1. Open voting session
        String sessionJson = """
            {
                "durationMinutes": 2
            }
            """;
        
        client.post()
                .uri("/api/agendas/{agendaId}/voting-session", testAgendaId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(sessionJson)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.agendaId").isEqualTo(testAgendaId)
                .jsonPath("$.durationMinutes").isEqualTo(2);

        // 2. Submit vote with a real valid CPF (11144477735 is a valid CPF)
        String voteJson = """
            {
                "cpf": "11144477735",
                "vote": "Yes"
            }
            """;

        client.post()
                .uri("/api/agendas/{agendaId}/votes", testAgendaId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(voteJson)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.agendaId").isEqualTo(testAgendaId)
                .jsonPath("$.cpf").isEqualTo("11144477735")
                .jsonPath("$.vote").isEqualTo("Yes");

        // 3. Get results to verify the vote was counted
        client.get()
                .uri("/api/agendas/{agendaId}/results", testAgendaId)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.agendaId").isEqualTo(testAgendaId)
                .jsonPath("$.yesVotes").isEqualTo(1)
                .jsonPath("$.noVotes").isEqualTo(0)
                .jsonPath("$.status").isEqualTo("Open");
    }

    @Test
    void testVotingWithInvalidCpfReturns404() {
        // Agenda is already created in setUp()
        // Create session first
        String sessionJson = """
            {
                "durationMinutes": 2
            }
            """;
        
        client.post()
                .uri("/api/agendas/{agendaId}/voting-session", testAgendaId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(sessionJson)
                .exchange()
                .expectStatus().isCreated();
        
        // Use an invalid CPF that would fail real validation
        String invalidCpfJson = """
            {
                "cpf": "12345678901",
                "vote": "Yes"
            }
            """;
        
        client.post()
                .uri("/api/agendas/{agendaId}/votes", testAgendaId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(invalidCpfJson)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.error").isEqualTo("CPF is not able to vote");
    }
}
