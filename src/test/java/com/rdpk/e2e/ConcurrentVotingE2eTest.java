package com.rdpk.e2e;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.MediaType;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static com.rdpk.e2e.helpers.VoteTestData.*;

/**
 * Tests concurrent voting scenarios to ensure thread safety.
 * Uses default lenient profile for CPF validation.
 */
class ConcurrentVotingE2eTest extends AbstractE2eTest {

    private Long agendaId;

    @BeforeEach
    void setUp() {
        super.setUp();
        
        // Create a fresh agenda for each test
        agendaId = createTestAgenda("Concurrent Test Agenda", "Test Description");
    }

    @ParameterizedTest
    @MethodSource("com.rdpk.e2e.helpers.VoteTestData#multipleVotersProvider")
    void testMultipleVotersConcurrently(List<VoteData> votes, int expectedYesVotes, int expectedNoVotes) {
        // Create session first
        String sessionJson = createSessionJson(5); // 5 minutes to allow concurrent voting
        client.post()
                .uri("/api/v1/agendas/{agendaId}/voting-session", agendaId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(sessionJson)
                .exchange()
                .expectStatus().isCreated();

        // Submit all votes concurrently
        ExecutorService executor = Executors.newFixedThreadPool(votes.size());
        List<CompletableFuture<Void>> futures = votes.stream()
                .map(vote -> CompletableFuture.runAsync(() -> {
                    String voteJson = createVoteJson(vote.cpf(), vote.vote());
                    client.post()
                            .uri("/api/v1/agendas/{agendaId}/votes", agendaId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(voteJson)
                            .exchange()
                            .expectStatus().isCreated();
                }, executor))
                .toList();

        // Wait for all votes to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .orTimeout(5, TimeUnit.SECONDS)
                .join();

        executor.shutdown();

        // Verify final results
        client.get()
                .uri("/api/v1/agendas/{agendaId}/results", agendaId)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.agendaId").isEqualTo(agendaId)
                .jsonPath("$.yesVotes").isEqualTo(expectedYesVotes)
                .jsonPath("$.noVotes").isEqualTo(expectedNoVotes)
                .jsonPath("$.status").isEqualTo("Open");
    }

    @Test
    void testConcurrentDuplicateVoteAttempts() {
        // Create session first
        String sessionJson = createSessionJson(2);
        client.post()
                .uri("/api/v1/agendas/{agendaId}/voting-session", agendaId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(sessionJson)
                .exchange()
                .expectStatus().isCreated();

        String cpf = "11144477735";
        String voteJson = createVoteJson(cpf, "Yes");

        // Multiple threads try to vote with same CPF
        ExecutorService executor = Executors.newFixedThreadPool(5);
        List<CompletableFuture<Void>> futures = IntStream.range(0, 5)
                .mapToObj(i -> CompletableFuture.runAsync(() -> {
                    client.post()
                            .uri("/api/v1/agendas/{agendaId}/votes", agendaId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(voteJson)
                            .exchange();
                }, executor))
                .toList();

        // Wait for all attempts to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .orTimeout(10, TimeUnit.SECONDS)
                .join();

        executor.shutdown();

        // Only one vote should succeed, others should fail
        // Check results - should have exactly 1 vote
        client.get()
                .uri("/api/v1/agendas/{agendaId}/results", agendaId)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.yesVotes").isEqualTo(1)
                .jsonPath("$.noVotes").isEqualTo(0);
    }

    @Test
    void testConcurrentVotingWithDifferentCpfs() {
        // Create session first
        String sessionJson = createSessionJson(3);
        client.post()
                .uri("/api/v1/agendas/{agendaId}/voting-session", agendaId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(sessionJson)
                .exchange()
                .expectStatus().isCreated();

        // Create 10 different CPFs voting "Yes"
        List<CompletableFuture<Void>> yesVotes = IntStream.range(0, 10)
                .mapToObj(i -> CompletableFuture.runAsync(() -> {
                    String cpf = String.format("111444777%02d", i);
                    String voteJson = createVoteJson(cpf, "Yes");
                    client.post()
                            .uri("/api/v1/agendas/{agendaId}/votes", agendaId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(voteJson)
                            .exchange()
                            .expectStatus().isCreated();
                }))
                .toList();

        // Create 5 different CPFs voting "No"
        List<CompletableFuture<Void>> noVotes = IntStream.range(0, 5)
                .mapToObj(i -> CompletableFuture.runAsync(() -> {
                    String cpf = String.format("123456789%02d", i);
                    String voteJson = createVoteJson(cpf, "No");
                    client.post()
                            .uri("/api/v1/agendas/{agendaId}/votes", agendaId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(voteJson)
                            .exchange()
                            .expectStatus().isCreated();
                }))
                .toList();

        // Wait for all votes to complete
        List<CompletableFuture<Void>> allVotes = new ArrayList<>();
        allVotes.addAll(yesVotes);
        allVotes.addAll(noVotes);
        
        CompletableFuture.allOf(allVotes.toArray(new CompletableFuture[0]))
                .orTimeout(5, TimeUnit.SECONDS)
                .join();

        // Verify final results: 10 Yes, 5 No
        client.get()
                .uri("/api/v1/agendas/{agendaId}/results", agendaId)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.agendaId").isEqualTo(agendaId)
                .jsonPath("$.yesVotes").isEqualTo(10)
                .jsonPath("$.noVotes").isEqualTo(5)
                .jsonPath("$.status").isEqualTo("Open");
    }

    @Test
    void testConcurrentResultsAccess() {
        // Create session first
        String sessionJson = createSessionJson(2);
        client.post()
                .uri("/api/v1/agendas/{agendaId}/voting-session", agendaId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(sessionJson)
                .exchange()
                .expectStatus().isCreated();

        // Submit some votes
        String voteJson1 = createVoteJson("11144477735", "Yes");
        String voteJson2 = createVoteJson("12345678901", "No");
        
        client.post()
                .uri("/api/v1/agendas/{agendaId}/votes", agendaId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(voteJson1)
                .exchange()
                .expectStatus().isCreated();

        client.post()
                .uri("/api/v1/agendas/{agendaId}/votes", agendaId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(voteJson2)
                .exchange()
                .expectStatus().isCreated();

        // Multiple threads access results concurrently
        ExecutorService executor = Executors.newFixedThreadPool(10);
        List<CompletableFuture<Void>> futures = IntStream.range(0, 10)
                .mapToObj(i -> CompletableFuture.runAsync(() -> {
                    client.get()
                            .uri("/api/v1/agendas/{agendaId}/results", agendaId)
                            .exchange()
                            .expectStatus().isOk()
                            .expectBody()
                            .jsonPath("$.yesVotes").isEqualTo(1)
                            .jsonPath("$.noVotes").isEqualTo(1);
                }, executor))
                .toList();

        // Wait for all requests to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .orTimeout(10, TimeUnit.SECONDS)
                .join();

        executor.shutdown();
    }

    @Test
    void testConcurrentVotingAndResultsAccess() {
        // Create session first
        String sessionJson = createSessionJson(3);
        client.post()
                .uri("/api/v1/agendas/{agendaId}/voting-session", agendaId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(sessionJson)
                .exchange()
                .expectStatus().isCreated();

        // Start voting and checking results concurrently
        ExecutorService executor = Executors.newFixedThreadPool(8);
        
        // Voting threads
        List<CompletableFuture<Void>> votingThreads = IntStream.range(0, 5)
                .mapToObj(i -> CompletableFuture.runAsync(() -> {
                    String cpf = String.format("111444777%02d", i);
                    String voteJson = createVoteJson(cpf, "Yes");
                    client.post()
                            .uri("/api/v1/agendas/{agendaId}/votes", agendaId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(voteJson)
                            .exchange()
                            .expectStatus().isCreated();
                }, executor))
                .toList();

        // Results checking threads
        List<CompletableFuture<Void>> resultsThreads = IntStream.range(0, 3)
                .mapToObj(i -> CompletableFuture.runAsync(() -> {
                    client.get()
                            .uri("/api/v1/agendas/{agendaId}/results", agendaId)
                            .exchange()
                            .expectStatus().isOk();
                }, executor))
                .toList();

        // Wait for all operations to complete
        List<CompletableFuture<Void>> allOperations = new ArrayList<>();
        allOperations.addAll(votingThreads);
        allOperations.addAll(resultsThreads);
        
        CompletableFuture.allOf(allOperations.toArray(new CompletableFuture[0]))
                .orTimeout(5, TimeUnit.SECONDS)
                .join();

        executor.shutdown();

        // Verify final state
        client.get()
                .uri("/api/v1/agendas/{agendaId}/results", agendaId)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.yesVotes").isEqualTo(5)
                .jsonPath("$.noVotes").isEqualTo(0);
    }
}
