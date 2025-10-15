    # Voting System API

[![CI](https://github.com/rodolfo/desafio-votacao/actions/workflows/ci.yml/badge.svg)](https://github.com/rodolfo/desafio-votacao/actions/workflows/ci.yml)

A comprehensive voting system API built with Spring Boot, featuring agenda management, voting sessions, and real-time results.

## Features

- **Agenda Management**: Create and manage voting agendas
- **Voting Sessions**: Open and manage voting sessions with configurable duration
- **Vote Submission**: Submit votes with CPF validation
- **Real-time Results**: Get voting results with live status updates
- **CPF Validation**: Configurable CPF validation (lenient/strict modes)
- **Concurrent Voting**: Thread-safe voting with duplicate prevention
- **Session Expiration**: Automatic session closure with time-based validation
- **Comprehensive Testing**: Unit tests, E2E tests, and architecture tests

## Requirements

* Java 25
* Maven (tested with 3.9.3)

## API Endpoints

### Agendas
- `POST /api/agendas` - Create a new agenda
- `GET /api/agendas/{id}` - Get agenda details

### Voting Sessions
- `POST /api/agendas/{agendaId}/voting-session` - Open a voting session
- `GET /api/agendas/{agendaId}/voting-session` - Get session details

### Voting
- `POST /api/agendas/{agendaId}/votes` - Submit a vote
- `GET /api/agendas/{agendaId}/votes` - Get all votes for an agenda

### Results
- `GET /api/agendas/{agendaId}/results` - Get voting results
- `GET /api/agendas/{agendaId}/results/stream` - Stream real-time results

### CPF Validation
- `GET /api/cpf-validation/{cpf}` - Validate a CPF

## Running

### Local Development

1. On terminal, run:
    ```bash
        ./mvnw clean spring-boot:run
    ``` 
2. On browser, open: http://localhost:8080/webjars/swagger-ui/index.html
3. Then play with the swagger-ui in order to test the voting system

### Running Tests

```bash
# Run all tests
./mvnw test

# Run only unit tests
./mvnw test -Dtest="!*E2eTest"

# Run only E2E tests
./mvnw test -Dtest="*E2eTest"

# Generate coverage report
./mvnw test jacoco:report

# Or use the convenience script
./generate-coverage.sh

# Generate coverage badge locally
./generate-coverage-badge.sh

# View coverage report
open target/site/jacoco/index.html
```


## CI/CD

Comprehensive GitHub Actions workflows with:
- **Java 25 Testing**: Tests on latest LTS Java 25
- **Code Coverage**: JaCoCo reports generated locally
- **Test Reporting**: Detailed JUnit XML reports
- **Build Artifacts**: JAR files for deployment

## Architecture

Domain-driven design with clear separation of concerns:
- **Features**: Organized by business capabilities (agenda, session, voting)
- **Domain**: Core business logic and entities
- **Infrastructure**: External concerns (time, validation)

### Key Components
- **Agenda**: Represents a voting topic
- **VotingSession**: Manages voting time windows
- **Vote**: Individual vote submissions
- **VotingResult**: Aggregated voting results
- **TimeProvider**: Abstraction for time-dependent operations
- **CpfValidationService**: CPF validation abstraction

## Configuration

### CPF Validation Modes
- **Lenient Mode** (default): Accepts all CPF formats for testing
- **Strict Mode**: Validates CPF format and eligibility using caelum-stella

### Profiles
- **default**: Lenient CPF validation, development settings
- **test**: Default test configuration
- **strict**: Strict CPF validation for validation tests
- **session-expiration**: Mock time provider for session tests

## Notes

* The code is fully tested with comprehensive test coverage
* It includes [`ArchUnit`](https://www.archunit.org/use-cases) tests to assert basic architecture
* It has extensive E2E tests covering all major workflows
* It includes concurrent voting tests to ensure thread safety
* For production deployment, consider adding caching, circuit breakers, rate limiting, logging, database persistence, and observability