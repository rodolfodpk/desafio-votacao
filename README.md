# Voting System API

[![Java CI](https://github.com/rodolfodpk/desafio-votacao/actions/workflows/ci.yml/badge.svg)](https://github.com/rodolfodpk/desafio-votacao/actions/workflows/ci.yml)
[![Coverage](.github/badges/jacoco.svg)](https://github.com/rodolfodpk/desafio-votacao/actions/workflows/ci.yml)
[![Branches](.github/badges/branches.svg)](https://github.com/rodolfodpk/desafio-votacao/actions/workflows/ci.yml)
[![Java](https://img.shields.io/badge/Java-25-orange?logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/25/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.6-brightgreen?logo=spring&logoColor=white)](https://spring.io/projects/spring-boot)
[![Maven](https://img.shields.io/badge/Maven-3.9.3-blue?logo=apachemaven&logoColor=white)](https://maven.apache.org/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-17.2-blue?logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

A voting system API built with Spring Boot, featuring agenda management, voting sessions, and results tracking.

## Features

- **Agenda Management**: Create and manage voting agendas
- **Voting Sessions**: Open and manage voting sessions with configurable duration
- **Vote Submission**: Submit votes with CPF validation
- **Voting Results**: Get voting results with current status
- **CPF Validation**: Configurable CPF validation (lenient/strict modes)
- **Concurrent Voting**: Thread-safe voting with duplicate prevention
- **Session Expiration**: Automatic session closure with time-based validation
- **Testing**: Unit tests, E2E tests, and architecture tests

## Requirements

* Java 25
* Maven (tested with 3.9.3)
* PostgreSQL 17.2+ (or use Docker Compose)
* Docker & Docker Compose (for containerized deployment)
* k6 (for performance testing)

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
# Build the application (Testcontainers handles database for tests)
mvn clean package

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

### Makefile Commands

```bash
# Application Management
make start          # Start PostgreSQL and application
make start-k6       # Start with k6 testing profile
make stop           # Stop application and PostgreSQL
make restart        # Restart application and PostgreSQL
make logs           # Show application logs
make health         # Check application health

# Testing
make test           # Run unit and integration tests
make k6-test        # Run all k6 performance tests
make k6-test-automated # Automated k6 workflow
make k6-test-individual # Run each k6 test individually

# Individual k6 Tests
make k6-smoke       # Basic validation
make k6-load        # Normal load
make k6-stress      # Breaking point
make k6-spike       # Traffic surge
make k6-concurrent  # Race conditions
make k6-mixed       # Realistic simulation
make k6-duplicate   # Duplicate vote handling
make k6-expiration  # Session expiration

# Development
make clean          # Clean build artifacts and containers
make build          # Build the application
make setup          # Initial setup
```

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
- **Java 25 Testing**: Tests on Java 25
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
- **k6**: Optimized for performance testing with relaxed Resilience4j settings

### Performance Testing

The project includes 8 k6 performance test scenarios:

- **Smoke Test** - Basic functionality validation
- **Load Test** - Normal expected load simulation
- **Stress Test** - Breaking point testing
- **Spike Test** - Traffic surge testing
- **Concurrent Test** - Race conditions and database consistency
- **Mixed Workload** - Realistic production simulation
- **Duplicate Vote Test** - Duplicate vote handling
- **Session Expiration Test** - Session expiration validation

See [k6/README.md](k6/README.md) for detailed usage instructions and [benchmark-results.md](benchmark-results.md) for test results.

## Notes

* The code has 85.8% instruction coverage and 63.2% branch coverage
* It includes [`ArchUnit`](https://www.archunit.org/use-cases) tests to assert basic architecture
* It has E2E tests covering major workflows using Testcontainers
* It includes concurrent voting tests to ensure thread safety
* Uses PostgreSQL with R2DBC for reactive, non-blocking database access
* Flyway manages database schema migrations automatically
* E2E tests run against real PostgreSQL instances via Testcontainers