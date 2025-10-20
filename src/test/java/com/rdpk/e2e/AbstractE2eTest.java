package com.rdpk.e2e;

import com.rdpk.config.TestConfig;
import com.rdpk.e2e.config.SharedPostgresContainer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Abstract base class for all E2E tests.
 * 
 * Provides:
 * - PostgreSQL Testcontainer for isolated database testing
 * - Common test configuration with @TestInstance(PER_CLASS) for performance
 * - Automatic database cleanup before each test
 * - Shared WebTestClient setup
 * - ObjectMapper for JSON serialization/deserialization
 * - Common test utilities
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Import(TestConfig.class)
public abstract class AbstractE2eTest {

    protected static PostgreSQLContainer<?> postgres = SharedPostgresContainer.getInstance();

    static {
        postgres.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // Container is already started in static block
        // R2DBC configuration for reactive database access
        String r2dbcUrl = String.format("r2dbc:postgresql://%s:%d/%s",
            postgres.getHost(),
            postgres.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT),
            postgres.getDatabaseName()
        );
        registry.add("spring.r2dbc.url", () -> r2dbcUrl);
        registry.add("spring.r2dbc.username", postgres::getUsername);
        registry.add("spring.r2dbc.password", postgres::getPassword);
        
        // Flyway configuration for migrations (uses JDBC)
        registry.add("spring.flyway.url", postgres::getJdbcUrl);
        registry.add("spring.flyway.user", postgres::getUsername);
        registry.add("spring.flyway.password", postgres::getPassword);
    }

    @LocalServerPort
    protected int port;
    
    protected WebTestClient client;
    
    @Autowired
    protected ObjectMapper objectMapper;
    
    @Autowired
    protected DatabaseClient databaseClient;

    @BeforeEach
    void setUp() {
        // Clear all tables before each test to ensure isolation
        // Use TRUNCATE for faster, more reliable cleanup with automatic sequence reset
        databaseClient.sql("TRUNCATE TABLE agendas, voting_sessions, votes RESTART IDENTITY CASCADE")
                .fetch()
                .rowsUpdated()
                .block();
        
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
        
        String response = client.post()
                .uri("/api/v1/agendas")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .bodyValue(agendaJson)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();
        
        // Parse the JSON response to get the ID
        try {
            return objectMapper.readTree(response).get("id").asLong();
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse agenda creation response", e);
        }
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
                .uri("/api/v1/agendas/{agendaId}/voting-session", agendaId)
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .bodyValue(sessionJson)
                .exchange()
                .expectStatus().isCreated();
    }
}
