package com.rdpk.e2e;

import com.rdpk.e2e.config.TimeProviderTestConfig;
import com.rdpk.e2e.helpers.FixedTimeProvider;
import com.rdpk.infrastructure.time.TimeProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static com.rdpk.e2e.helpers.VoteTestData.*;

/**
 * Tests session expiration scenarios using mock time provider.
 * Uses "session-expiration" profile with FixedTimeProvider for reliable time control.
 */
@ActiveProfiles("session-expiration")
@Import(TimeProviderTestConfig.class)
class SessionExpirationE2eTest extends AbstractE2eTest {

    private Long agendaId;
    
    @Autowired
    private TimeProvider timeProvider;

    @BeforeEach
    void setUp() {
        super.setUp();
        
        // Reset time to a fixed point for each test
        ((FixedTimeProvider) timeProvider).setTime(LocalDateTime.of(2025, 1, 1, 10, 0));
        
        // Create a fresh agenda for each test
        agendaId = createTestAgenda("Expiration Test Agenda", "Test Description");
    }

    @Test
    void testVoteAfterSessionExpires() {
        // Create session with 1 minute duration
        String sessionJson = createSessionJson(1);
        client.post()
                .uri("/api/v1/agendas/{agendaId}/voting-session", agendaId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(sessionJson)
                .exchange()
                .expectStatus().isCreated();

        // Advance time by 2 minutes (session should be expired)
        ((FixedTimeProvider) timeProvider).advance(2);

        // Try to vote - should fail
        String voteJson = createVoteJson("11144477735", "Yes");
        client.post()
                .uri("/api/v1/agendas/{agendaId}/votes", agendaId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(voteJson)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.error").isEqualTo("Voting session is closed");
    }

    @Test
    void testResultsShowClosedAfterExpiration() {
        // Create session with 1 minute duration
        String sessionJson = createSessionJson(1);
        client.post()
                .uri("/api/v1/agendas/{agendaId}/voting-session", agendaId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(sessionJson)
                .exchange()
                .expectStatus().isCreated();

        // Vote successfully before expiration
        String voteJson = createVoteJson("11144477735", "Yes");
        client.post()
                .uri("/api/v1/agendas/{agendaId}/votes", agendaId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(voteJson)
                .exchange()
                .expectStatus().isCreated();

        // Advance time by 2 minutes (session should be expired)
        ((FixedTimeProvider) timeProvider).advance(2);

        // Get results after expiration
        client.get()
                .uri("/api/v1/agendas/{agendaId}/results", agendaId)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("Closed")
                .jsonPath("$.yesVotes").isEqualTo(1)
                .jsonPath("$.noVotes").isEqualTo(0);
    }

    @Test
    void testCannotReopenExpiredSession() {
        // Create session with 1 minute duration
        String sessionJson = createSessionJson(1);
        client.post()
                .uri("/api/v1/agendas/{agendaId}/voting-session", agendaId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(sessionJson)
                .exchange()
                .expectStatus().isCreated();

        // Advance time by 2 minutes (session should be expired)
        ((FixedTimeProvider) timeProvider).advance(2);

        // Try to open session again after expiration - should still fail due to existing session
        String newSessionJson = createSessionJson(2);
        client.post()
                .uri("/api/v1/agendas/{agendaId}/voting-session", agendaId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(newSessionJson)
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectBody()
                .jsonPath("$.error").isEqualTo("Voting session already exists");
    }

    @Test
    void testVoteJustBeforeExpiration() {
        // Create session with 5 minutes duration
        String sessionJson = createSessionJson(5);
        client.post()
                .uri("/api/v1/agendas/{agendaId}/voting-session", agendaId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(sessionJson)
                .exchange()
                .expectStatus().isCreated();

        // Vote immediately
        String voteJson1 = createVoteJson("11144477735", "Yes");
        client.post()
                .uri("/api/v1/agendas/{agendaId}/votes", agendaId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(voteJson1)
                .exchange()
                .expectStatus().isCreated();

        // Advance time by 3 minutes (still within session)
        ((FixedTimeProvider) timeProvider).advance(3);

        // Vote again - should still work
        String voteJson2 = createVoteJson("12345678901", "No");
        client.post()
                .uri("/api/v1/agendas/{agendaId}/votes", agendaId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(voteJson2)
                .exchange()
                .expectStatus().isCreated();

        // Check results
        client.get()
                .uri("/api/v1/agendas/{agendaId}/results", agendaId)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("Open")
                .jsonPath("$.yesVotes").isEqualTo(1)
                .jsonPath("$.noVotes").isEqualTo(1);
    }

    @Test
    void testSessionDefaultDuration() {
        // Create session without specifying duration (should default to 1 minute)
        String sessionJson = "{}";
        client.post()
                .uri("/api/v1/agendas/{agendaId}/voting-session", agendaId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(sessionJson)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.durationMinutes").isEqualTo(1);

        // Vote should work immediately
        String voteJson = createVoteJson("11144477735", "Yes");
        client.post()
                .uri("/api/v1/agendas/{agendaId}/votes", agendaId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(voteJson)
                .exchange()
                .expectStatus().isCreated();
    }

    @Test
    void testMultipleVotesBeforeExpiration() {
        // Create session with 10 minutes duration
        String sessionJson = createSessionJson(10);
        client.post()
                .uri("/api/v1/agendas/{agendaId}/voting-session", agendaId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(sessionJson)
                .exchange()
                .expectStatus().isCreated();

        // Vote multiple times
        String[] cpfs = {"11144477735", "12345678901", "98765432100", "55566677788"};
        String[] votes = {"Yes", "No", "Yes", "No"};

        for (int i = 0; i < cpfs.length; i++) {
            String voteJson = createVoteJson(cpfs[i], votes[i]);
            client.post()
                    .uri("/api/v1/agendas/{agendaId}/votes", agendaId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(voteJson)
                    .exchange()
                    .expectStatus().isCreated();
        }

        // Check results
        client.get()
                .uri("/api/v1/agendas/{agendaId}/results", agendaId)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("Open")
                .jsonPath("$.yesVotes").isEqualTo(2)
                .jsonPath("$.noVotes").isEqualTo(2);
    }
}
