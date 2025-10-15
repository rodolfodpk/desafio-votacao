# Voting System API

[![Tests](https://github.com/rodolfo/desafio-votacao/actions/workflows/tests.yml/badge.svg)](https://github.com/rodolfo/desafio-votacao/actions/workflows/tests.yml)
[![Build](https://github.com/rodolfo/desafio-votacao/actions/workflows/build.yml/badge.svg)](https://github.com/rodolfo/desafio-votacao/actions/workflows/build.yml)
[![CI](https://github.com/rodolfo/desafio-votacao/actions/workflows/ci.yml/badge.svg)](https://github.com/rodolfo/desafio-votacao/actions/workflows/ci.yml)
[![codecov](https://codecov.io/gh/rodolfo/desafio-votacao/branch/main/graph/badge.svg)](https://codecov.io/gh/rodolfo/desafio-votacao)

A comprehensive voting system API built with Spring Boot, featuring agenda management, voting sessions, and real-time results.

## Features

- ✅ **Agenda Management**: Create and manage voting agendas
- ✅ **Voting Sessions**: Open and manage voting sessions with configurable duration
- ✅ **Vote Submission**: Submit votes with CPF validation
- ✅ **Real-time Results**: Get voting results with live status updates
- ✅ **CPF Validation**: Configurable CPF validation (lenient/strict modes)
- ✅ **Concurrent Voting**: Thread-safe voting with duplicate prevention
- ✅ **Session Expiration**: Automatic session closure with time-based validation
- ✅ **Comprehensive Testing**: Unit tests, E2E tests, and architecture tests

## Requirements

* Java 21
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

# View coverage report
open target/site/jacoco/index.html
```

### Docker

```bash
# Build Docker image
docker build -t voting-system .

# Run with Docker
docker run -p 8080:8080 voting-system
```

## CI/CD

This project includes comprehensive GitHub Actions workflows:

- **Tests Workflow** (`tests.yml`): Runs unit and E2E tests separately with detailed reporting
- **Build Workflow** (`build.yml`): Builds the application and creates Docker images
- **CI Workflow** (`ci.yml`): Complete CI pipeline with security scanning

### Workflow Features

- ✅ **Multi-JDK Testing**: Tests on Java 17 and 21
- ✅ **Separate Test Jobs**: Unit tests and E2E tests run independently
- ✅ **Test Reporting**: Detailed test results with JUnit XML reports
- ✅ **Code Coverage**: JaCoCo coverage reports with Codecov integration
- ✅ **Coverage Badge**: Real-time coverage percentage in README
- ✅ **Docker Support**: Automated Docker image building and pushing
- ✅ **Security Scanning**: OWASP dependency vulnerability scanning
- ✅ **Caching**: Maven dependencies are cached for faster builds
- ✅ **Artifact Upload**: Test results and build artifacts are preserved

## Architecture

### Domain-Driven Design
The application follows DDD principles with clear separation of concerns:

- **Features**: Organized by business capabilities (agenda, session, voting)
- **Domain**: Core business logic and entities
- **Infrastructure**: External concerns (time, validation)
- **Configuration**: Application configuration and profiles

### Key Components

- **Agenda**: Represents a voting topic
- **VotingSession**: Manages voting time windows
- **Vote**: Individual vote submissions
- **VotingResult**: Aggregated voting results
- **TimeProvider**: Abstraction for time-dependent operations
- **CpfValidationService**: CPF validation abstraction

### Testing Strategy

- **Unit Tests**: Test individual components in isolation
- **E2E Tests**: Test complete workflows end-to-end
- **Architecture Tests**: Validate architectural constraints with ArchUnit
- **Concurrent Tests**: Verify thread safety and race conditions
- **Session Expiration Tests**: Test time-based functionality with mock time
- **Code Coverage**: JaCoCo integration with 80%+ coverage requirement

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
* For production deployment, consider adding:
  - Caching for frequently accessed data
  - Circuit breakers for external service calls
  - Rate limiting for API endpoints
  - Comprehensive logging and monitoring
  - Database persistence instead of in-memory storage
  - Container orchestration (Kubernetes)
  - Observability (metrics, tracing, alerting)

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests for new functionality
5. Ensure all tests pass
6. Submit a pull request

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.