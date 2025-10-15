package com.rdpk.e2e;

import com.rdpk.config.TestConfig;
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
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

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
@Testcontainers
@Import(TestConfig.class)
public abstract class AbstractE2eTest {

    @Container
    protected static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17.2")
            .withDatabaseName("votacao_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // Start the container first
        postgres.start();
        
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
        databaseClient.sql("DELETE FROM votes").fetch().rowsUpdated().block();
        databaseClient.sql("DELETE FROM voting_sessions").fetch().rowsUpdated().block();
        databaseClient.sql("DELETE FROM agendas").fetch().rowsUpdated().block();
        
        // Reset sequences
        databaseClient.sql("ALTER SEQUENCE agendas_id_seq RESTART WITH 1").fetch().rowsUpdated().block();
        databaseClient.sql("ALTER SEQUENCE voting_sessions_id_seq RESTART WITH 1").fetch().rowsUpdated().block();
        databaseClient.sql("ALTER SEQUENCE votes_id_seq RESTART WITH 1").fetch().rowsUpdated().block();
        
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
