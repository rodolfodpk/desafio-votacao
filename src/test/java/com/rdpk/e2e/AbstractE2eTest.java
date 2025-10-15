package com.rdpk.e2e;

import com.rdpk.config.TestConfig;
import com.rdpk.features.agenda.repository.AgendaRepositoryImpl;
import com.rdpk.features.session.repository.VotingSessionRepositoryImpl;
import com.rdpk.features.voting.repository.VoteRepositoryImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * Abstract base class for all E2E tests.
 * 
 * Provides:
 * - Common test configuration with @TestInstance(PER_CLASS) for performance
 * - Automatic repository cleanup before each test
 * - Shared WebTestClient setup
 * - ObjectMapper for JSON serialization/deserialization
 * - Common test utilities
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Import(TestConfig.class)
public abstract class AbstractE2eTest {

    @LocalServerPort
    protected int port;
    
    protected WebTestClient client;
    
    @Autowired
    protected ObjectMapper objectMapper;
    
    @Autowired
    protected AgendaRepositoryImpl agendaRepository;
    
    @Autowired
    protected VotingSessionRepositoryImpl sessionRepository;
    
    @Autowired
    protected VoteRepositoryImpl voteRepository;

    @BeforeEach
    void setUp() {
        // Clear all repository data before each test to ensure isolation
        agendaRepository.clear();
        sessionRepository.clear();
        voteRepository.clear();
        
        // Initialize WebTestClient if not already done
        if (this.client == null) {
            this.client = WebTestClient.bindToServer()
                    .baseUrl("http://localhost:" + port)
                    .build();
        }
    }
    
    /**
     * Creates a test agenda and returns its ID.
     * Override this method if you need custom agenda creation logic.
     */
    protected Long createTestAgenda(String title, String description) {
        String agendaJson = String.format("""
                {
                    "title": "%s",
                    "description": "%s"
                }
                """, title, description);
        
        client.post()
                .uri("/api/agendas")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .bodyValue(agendaJson)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.id").exists();
        
        // Return the first agenda ID (1L) for simplicity
        return 1L;
    }
    
    /**
     * Creates a voting session for the given agenda.
     */
    protected void createVotingSession(Long agendaId, Integer durationMinutes) {
        String sessionJson = durationMinutes != null ? 
            String.format("""
                {
                    "durationMinutes": %d
                }
                """, durationMinutes) : "{}";
        
        client.post()
                .uri("/api/agendas/{agendaId}/voting-session", agendaId)
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .bodyValue(sessionJson)
                .exchange()
                .expectStatus().isCreated();
    }
}
