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
* PostgreSQL 17.2+ (or use Docker Compose)
* Docker & Docker Compose (for containerized deployment)

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

## Database

The application uses PostgreSQL with R2DBC for reactive database access and Flyway for schema migrations.

### Database Schema

Flyway migrations are located in `src/main/resources/db/migration/` and will be automatically applied on startup:

- **V1__create_agendas_table.sql**: Creates the agendas table
- **V2__create_voting_sessions_table.sql**: Creates the voting_sessions table
- **V3__create_votes_table.sql**: Creates the votes table with unique constraint

### Flyway Management

```bash
# Check migration status
./mvnw flyway:info

# Validate migrations
./mvnw flyway:validate

# Clean database (WARNING: destructive)
./mvnw flyway:clean
```

## Running

### Option 1: Docker Compose (Recommended)

The easiest way to run the entire application with PostgreSQL:

```bash
# Start PostgreSQL and application
docker-compose up

# Or run in detached mode
docker-compose up -d

# View logs
docker-compose logs -f

# Stop all services
docker-compose down

# Stop and remove volumes (clean slate)
docker-compose down -v
```

Access the application at http://localhost:8080

### Option 2: PostgreSQL Only with Local Application

Run PostgreSQL in Docker and the application locally for development:

```bash
# Start only PostgreSQL
docker-compose up postgres

# In another terminal, run the application
./mvnw spring-boot:run
```

### Option 3: Local PostgreSQL

If you have PostgreSQL installed locally:

1. Create the database:
   ```bash
   psql -U postgres -c "CREATE DATABASE votacao;"
   psql -U postgres -c "CREATE USER votacao WITH PASSWORD 'votacao';"
   psql -U postgres -c "GRANT ALL PRIVILEGES ON DATABASE votacao TO votacao;"
   ```

2. Update `src/main/resources/application.properties` if your PostgreSQL credentials differ

3. Run the application:
   ```bash
   ./mvnw spring-boot:run
   ```

### Accessing the API

Once running, open the Swagger UI in your browser:
- http://localhost:8080/webjars/swagger-ui/index.html

Then play with the swagger-ui to test the voting system.

### Running Tests

**Note**: E2E tests use Testcontainers to spin up PostgreSQL automatically. Docker must be running.

```bash
# Run all tests (includes Testcontainers)
./mvnw test

# Run only unit tests
./mvnw test -Dtest="!*E2eTest"

# Run only E2E tests (requires Docker)
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
* It has extensive E2E tests covering all major workflows using Testcontainers
* It includes concurrent voting tests to ensure thread safety
* Uses PostgreSQL with R2DBC for reactive, non-blocking database access
* Flyway manages database schema migrations automatically
* E2E tests run against real PostgreSQL instances via Testcontainers
* For production deployment, consider adding:
  - Connection pooling tuning
  - Database indexing optimization
  - Caching layer (Redis)
  - Circuit breakers and rate limiting
  - Enhanced logging and observability
  - Backup and disaster recovery strategies