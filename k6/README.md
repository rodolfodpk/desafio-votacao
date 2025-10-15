# K6 Performance Tests

This directory contains performance tests for the Voting System API using [k6](https://k6.io/).

## Overview

Tests for the voting system's performance and load handling. It tests the core REST API endpoints:

- `POST /api/agendas` - Create agenda
- `POST /api/agendas/{agendaId}/voting-session` - Open voting session  
- `POST /api/agendas/{agendaId}/votes` - Submit vote
- `GET /api/agendas/{agendaId}/results` - Get voting results

## Quick Start

### Prerequisites

1. **Install k6**: [k6 Installation Guide](https://k6.io/docs/get-started/installation/)
2. **Docker & Docker Compose**: For PostgreSQL database
3. **Java 25 & Maven**: For the application

### Running Tests

```bash
# Start the application
make start-k6

# Individual tests with cleanup
make k6-test-individual

# Run individual tests
make k6-smoke      # Basic validation
make k6-load       # Normal load
make k6-stress     # Breaking point
make k6-spike      # Traffic surge
make k6-concurrent # Race conditions
make k6-mixed      # Realistic simulation
make k6-duplicate  # Duplicate vote handling
make k6-expiration # Session expiration

# Run all tests
make k6-test
```

## Test Scenarios

### 1. Smoke Test (`smoke-test.js`)
**Purpose**: Basic sanity check with minimal load
- **VUs**: 5
- **Duration**: 1 minute
- **Focus**: Verify all endpoints work correctly
- **Success Criteria**: 100% success rate, p95 < 500ms

```bash
make k6-smoke
```

### 2. Load Test (`load-test.js`)
**Purpose**: Normal expected load simulation
- **VUs**: 0 → 50 → 0 (ramp up/down)
- **Duration**: 9 minutes
- **Test Mix**: 5% create, 5% open session, 80% vote, 10% results
- **Success Criteria**: 99% success rate, p95 < 1s, >100 req/s

```bash
make k6-load
```

### 3. Stress Test (`stress-test.js`)
**Purpose**: Find system breaking point
- **VUs**: 0 → 200 → 300 → 0
- **Duration**: 14 minutes
- **Focus**: Concurrent voting on same agenda
- **Success Criteria**: Identify max users, <5% error rate at peak

```bash
make k6-stress
```

### 4. Spike Test (`spike-test.js`)
**Purpose**: Sudden traffic surge resilience
- **VUs**: 10 → 200 → 10 (sudden spike)
- **Duration**: 5.5 minutes
- **Use Case**: Viral voting campaign simulation
- **Success Criteria**: No crashes, <10% error during spike, recovery <30s

```bash
make k6-spike
```

### 5. Concurrent Test (`concurrent-voting.js`)
**Purpose**: Database consistency under high concurrency
- **VUs**: 100 concurrent
- **Duration**: 2 minutes
- **Focus**: Race condition testing, duplicate vote handling
- **Success Criteria**: Vote count matches unique CPFs, no lost votes

```bash
make k6-concurrent
```

### 6. Mixed Workload Test (`mixed-workload.js`)
**Purpose**: Realistic production simulation
- **VUs**: 30 steady
- **Duration**: 10 minutes
- **Test Mix**: 70% hot agendas, 20% medium agendas, 10% result checks
- **Success Criteria**: Realistic traffic patterns, 99% success rate

```bash
make k6-mixed
```

### 7. Duplicate Vote Test (`duplicate-vote-test.js`)
**Purpose**: Validate duplicate vote handling
- **VUs**: 3 concurrent
- **Duration**: 30 seconds
- **Focus**: Same CPF voting multiple times
- **Success Criteria**: Duplicate votes rejected

```bash
make k6-duplicate
```

### 8. Session Expiration Test (`session-expiration-test.js`)
**Purpose**: Test voting on expired sessions
- **VUs**: 2 concurrent
- **Duration**: 2 minutes
- **Focus**: Session expiration logic
- **Success Criteria**: Votes rejected after session expiration

```bash
make k6-expiration
```

## Configuration

### Environment Variables

```bash
# Base URL for the API
export BASE_URL=http://localhost:8080

# Test data configuration
export AGENDA_COUNT=10
export CPF_COUNT=1000
export YES_PERCENTAGE=60.0
export NO_PERCENTAGE=40.0

# Run test with custom configuration
k6 run -e BASE_URL=http://localhost:8080 k6/scripts/load-test.js
```

### Custom Metrics

The tests track custom business metrics:

- `vote_submissions` - Total vote submissions
- `vote_duplicates` - Duplicate vote attempts
- `vote_latency` - Vote submission latency
- `vote_success_rate` - Vote success rate
- `rate_limit_hits` - Rate limiter activations
- `circuit_breaker_opens` - Circuit breaker activations
- `database_consistency` - Database consistency checks

## Performance Targets

| Metric | Target | Excellent |
|--------|--------|-----------|
| Vote submission (p95) | < 500ms | < 200ms |
| Get results (p95) | < 300ms | < 100ms |
| Throughput | > 100 req/s | > 500 req/s |
| Concurrent users | > 100 | > 500 |
| Error rate | < 1% | < 0.1% |

## Resilience Testing

The tests validate Resilience4j patterns:

### Circuit Breaker Testing
- Triggers circuit breaker with repeated failures
- Verifies fast-fail behavior (< 10ms response)
- Validates automatic recovery

### Rate Limiter Testing  
- Sends > 100 req/s to trigger rate limiting
- Verifies 429 responses are returned
- Checks system stability under rate limiting

### Retry Testing
- Injects transient failures
- Verifies automatic retries succeed
- Monitors retry metrics

### Timeout Testing
- Tests long-running operations
- Verifies timeouts trigger correctly
- Checks resource cleanup

## Reporting

### HTML Reports
```bash
# Generate HTML report
k6 run --out html=reports/load-test.html k6/scripts/load-test.js
```

### JSON Output
```bash
# Generate JSON report
k6 run --out json=reports/load-test.json k6/scripts/load-test.js
```

### InfluxDB Integration
```bash
# Send metrics to InfluxDB for Grafana dashboards
k6 run --out influxdb=http://localhost:8086/k6 k6/scripts/load-test.js
```

## Troubleshooting

### Common Issues

1. **Application not running**
   ```bash
   # Check if application is running
   make health
   
   # Start application with k6 profile
   make start-k6
   ```

2. **Database connection issues**
   ```bash
   # Reset database
   make db-reset
   
   # Quick database cleanup (keeps app running)
   make db-clean-quick
   
   # Check PostgreSQL logs
   docker-compose logs postgres
   ```

3. **Port conflicts**
   ```bash
   # Stop all services
   make stop
   
   # Check for port usage
   lsof -i :8080
   lsof -i :5432
   ```

4. **k6 not found**
   ```bash
   # Install k6
   # macOS
   brew install k6
   
   # Linux
   sudo gpg -k
   sudo gpg --no-default-keyring --keyring /usr/share/keyrings/k6-archive-keyring.gpg --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys C5AD17C747E3415A3642D57D77C6C491D6AC1D69
   echo "deb [signed-by=/usr/share/keyrings/k6-archive-keyring.gpg] https://dl.k6.io/deb stable main" | sudo tee /etc/apt/sources.list.d/k6.list
   sudo apt-get update
   sudo apt-get install k6
   ```

5. **High error rates during load testing**
   ```bash
   # Check if database needs cleanup (common cause)
   make db-clean-quick
   
   # Check resilience4j metrics
   curl http://localhost:8080/actuator/circuitbreakers
   curl http://localhost:8080/actuator/ratelimiters
   
   # Restart with k6 profile (optimized settings)
   make stop && make start-k6
   ```

6. **Session expiration test failing**
   ```bash
   # Session expiration test requires 70+ seconds to complete
   # This is expected behavior - the test waits for real session expiration
   make k6-expiration
   ```

7. **Duplicate vote test showing high error rate**
   ```bash
   # 39.53% error rate is EXPECTED for duplicate vote test
   # These are 400 Bad Request responses for duplicate votes (correct behavior)
   make k6-duplicate
   ```

### Debug Mode

```bash
# Run with verbose logging
k6 run --verbose k6/scripts/smoke-test.js

# Run with debug output
k6 run --log-level=debug k6/scripts/smoke-test.js
```

## Development Workflow

### 1. Start Development Environment
```bash
make setup  # Clean, build, and start
```

### 2. Run Individual Tests
```bash
# Test each script individually with cleanup
make k6-test-individual

# Or run individual tests
make k6-smoke  # Basic validation
```

### 3. Run Full Test Suite
```bash
make k6-test  # All tests (takes ~40 minutes)
```

### 4. Monitor Application
```bash
make logs     # View application logs
make health   # Check health status

# Check resilience4j metrics
curl http://localhost:8080/actuator/circuitbreakers
curl http://localhost:8080/actuator/ratelimiters
```

### 5. Clean Up
```bash
make stop     # Stop application and database
make clean    # Clean all artifacts
```

## Testing Strategy

### Individual Testing with Cleanup
```bash
make k6-test-individual
```
- Tests each script individually
- Automatic database cleanup between tests
- Stops on first failure for easy debugging

### Database Management
- **`db-clean-quick`**: Fast cleanup without restarting application
- **`db-reset`**: Full database reset with application restart

## Test Checklist

Before running k6 tests, ensure:

- [ ] PostgreSQL is running via docker-compose
- [ ] Application starts successfully
- [ ] Flyway migrations completed
- [ ] Health endpoint responds (200 OK)
- [ ] Database is clean (no existing test data)
- [ ] k6 is installed and accessible

## Best Practices

1. **Always start with smoke test** - Validates basic setup
2. **Use fresh database** - Ensures clean test environment
3. **Monitor application logs** - Watch for errors during tests
4. **Check resilience metrics** - Verify circuit breakers, rate limiters
5. **Run tests in sequence** - Start with low load, increase gradually
6. **Document results** - Save reports for analysis

## Additional Resources

- [k6 Documentation](https://k6.io/docs/)
- [k6 JavaScript API](https://k6.io/docs/javascript-api/)
- [k6 Metrics](https://k6.io/docs/using-k6/metrics/)
- [Resilience4j Documentation](https://resilience4j.readme.io/)
- [Spring Boot Actuator](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)
