package com.rdpk.e2e.helpers;

import org.junit.jupiter.params.provider.Arguments;

import java.util.List;
import java.util.stream.Stream;

import static com.rdpk.e2e.helpers.CpfGenerator.*;

public class VoteTestData {

    public record VoteData(String cpf, String vote) {}

    public record VoteScenario(
            List<VoteData> votes,
            int expectedYesVotes,
            int expectedNoVotes
    ) {}

    // Data providers for parameterized tests
    public static Stream<Arguments> invalidCpfProvider() {
        return Stream.of(
                Arguments.of("", "CPF is required"),
                Arguments.of("123", "CPF must be 11 digits"),
                Arguments.of("1234567890", "CPF must be 11 digits"),
                Arguments.of("123456789012", "CPF must be 11 digits"),
                Arguments.of("1234567890123", "CPF must be 11 digits")
                // Note: "abc12345678" is accepted in lenient mode, so we don't test it here
        );
    }

    public static Stream<Arguments> duplicateVoteProvider() {
        String cpf1 = KnownValidCpfs.CPF_1;
        String cpf2 = KnownValidCpfs.CPF_2;
        
        return Stream.of(
                Arguments.of(cpf1, "Yes", "Yes"),
                Arguments.of(cpf1, "Yes", "No"),
                Arguments.of(cpf2, "No", "No"),
                Arguments.of(cpf2, "No", "Yes")
        );
    }

    public static Stream<Arguments> multipleVotersProvider() {
        return Stream.of(
                Arguments.of(
                        List.of(
                                new VoteData(KnownValidCpfs.CPF_1, "Yes"),
                                new VoteData(KnownValidCpfs.CPF_2, "No"),
                                new VoteData(KnownValidCpfs.CPF_3, "Yes"),
                                new VoteData(KnownValidCpfs.CPF_4, "No"),
                                new VoteData(KnownValidCpfs.CPF_5, "Yes")
                        ),
                        3, // expected yes votes
                        2  // expected no votes
                ),
                Arguments.of(
                        List.of(
                                new VoteData(KnownValidCpfs.CPF_1, "Yes"),
                                new VoteData(KnownValidCpfs.CPF_2, "Yes"),
                                new VoteData(KnownValidCpfs.CPF_3, "Yes"),
                                new VoteData(KnownValidCpfs.CPF_4, "Yes"),
                                new VoteData(KnownValidCpfs.CPF_5, "Yes")
                        ),
                        5, // expected yes votes
                        0  // expected no votes
                ),
                Arguments.of(
                        List.of(
                                new VoteData(KnownValidCpfs.CPF_1, "No"),
                                new VoteData(KnownValidCpfs.CPF_2, "No"),
                                new VoteData(KnownValidCpfs.CPF_3, "No")
                        ),
                        0, // expected yes votes
                        3  // expected no votes
                )
        );
    }

    public static Stream<Arguments> voteCountingAccuracyProvider() {
        return Stream.of(
                Arguments.of("Yes", 1, 0),
                Arguments.of("No", 0, 1),
                Arguments.of("Yes", 5, 3),
                Arguments.of("No", 2, 8)
        );
    }

    public static Stream<Arguments> sessionDurationProvider() {
        return Stream.of(
                Arguments.of(null, 1), // default duration
                Arguments.of(1, 1),
                Arguments.of(5, 5),
                Arguments.of(60, 60)
        );
    }

    // Helper methods for creating test data
    public static String createAgendaJson(String title, String description) {
        return String.format("""
                {
                    "title": "%s",
                    "description": "%s"
                }
                """, title, description);
    }

    public static String createSessionJson(Integer durationMinutes) {
        if (durationMinutes == null) {
            return "{}";
        }
        return String.format("""
                {
                    "durationMinutes": %d
                }
                """, durationMinutes);
    }

    public static String createVoteJson(String cpf, String vote) {
        return String.format("""
                {
                    "cpf": "%s",
                    "vote": "%s"
                }
                """, cpf, vote);
    }
    
    /**
     * Generates a random valid CPF for testing purposes.
     * Use this when you need a valid CPF but don't care about the specific value.
     */
    public static String generateRandomValidCpf() {
        return generateValidCpf();
    }
    
    /**
     * Gets a known valid CPF for testing purposes.
     * Use this when you need a predictable CPF value.
     */
    public static String getKnownValidCpf() {
        return KnownValidCpfs.getRandom();
    }
}
