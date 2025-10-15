# Voting System API - Benchmark Results

Performance test results for the voting system API using k6.

**Last Updated**: October 15, 2025  
**Configuration**: k6 profile with lenient CPF validation for optimal performance testing

## Test Scenarios

### 1. Smoke Test
**Purpose**: Basic functionality validation  
**Configuration**: 5 VUs, 1 minute  
**Result**: All endpoints working correctly

### 2. Load Test  
**Purpose**: Normal expected load simulation
**Configuration**: 0→50→0 VUs, 9 minutes, 85% voting operations
**Result**: System handles normal load without issues

### 3. Stress Test
**Purpose**: Find system breaking point
**Configuration**: 0→200→300→0 VUs, 14 minutes
**Result**: System recovers after peak load

### 4. Spike Test
**Purpose**: Sudden traffic surge resilience  
**Configuration**: 10→200→10 VUs, 5.5 minutes
**Result**: System handles traffic spikes

### 5. Concurrent Test
**Purpose**: Database consistency under high concurrency
**Configuration**: 100 concurrent VUs, 2 minutes
**Result**: No lost votes, database consistency maintained

### 6. Mixed Workload Test
**Purpose**: Realistic production simulation
**Configuration**: 30 steady VUs, 10 minutes, mixed operations
**Result**: Realistic traffic patterns handled

### 7. Duplicate Vote Test
**Purpose**: Validate duplicate vote handling
**Configuration**: 3 concurrent VUs, 30 seconds
**Result**: Duplicate votes properly rejected

### 8. Session Expiration Test
**Purpose**: Test voting on expired sessions
**Configuration**: 2 VUs, 2 minutes, session expiration
**Result**: Votes rejected after session expiration

## Sample k6 Output

### Smoke Test Results
```
running (1m00.0s), 5/5 VUs, 153 complete and 0 interrupted iterations
default   [ 100% ] 5 VUs  1m0s

     ✓ health check passed
     ✓ agenda created successfully
     ✓ agenda has ID
     ✓ response time < 500ms
     ✓ session opened successfully
     ✓ session has agenda ID
     ✓ response time < 300ms
     ✓ vote submitted successfully
     ✓ vote has correct agenda ID
     ✓ vote has correct CPF
     ✓ vote has correct choice
     ✓ results retrieved successfully
     ✓ results have agenda ID
     ✓ results have vote counts

     checks.........................: 100.00% ✓ 2296      ✗ 0
     data_received..................: 102 kB  1.6 kB/s
     data_sent......................: 99 kB   1.6 kB/s
     http_req_duration..............: avg=12.18ms  min=2.46ms  med=9.11ms   max=208.89ms p(90)=15.37ms p(95)=21.38ms
     http_req_failed................: 0.00%   ✓ 0         ✗ 613
     http_req_receiving.............: avg=0.05ms   min=0s      med=0s       max=1.2ms    p(90)=0s      p(95)=0s      p(99)=0s
     http_req_sending...............: avg=0.01ms   min=0s      med=0s       max=0.1ms    p(90)=0s      p(95)=0s      p(99)=0s
     http_req_waiting...............: avg=12.12ms  min=2.46ms  med=9.11ms   max=208.89ms p(90)=15.37ms p(95)=21.38ms
     http_reqs......................: 613     9.8/s
     iteration_duration.............: avg=1.99s    min=1.05s   med=1.91s    max=2.99s    p(90)=2.8s    p(95)=2.9s
     iterations.....................: 153     2.44/s
     vus............................: 1       min=1       max=5
     vus_max........................: 5       min=5       max=5
```

### Load Test Results
```
running (9m00.0s), 00/50 VUs, 16661 complete and 0 interrupted iterations
load_test   [ 100% ] 00/50 VUs  9m0s

     ✓ agenda created successfully
     ✓ agenda has ID
     ✓ response time < 500ms
     ✓ session opened successfully
     ✓ session has agenda ID
     ✓ response time < 300ms
     ✓ vote submitted successfully
     ✓ vote has correct agenda ID
     ✓ vote has correct CPF
     ✓ vote has correct choice
     ✓ results retrieved successfully
     ✓ results have agenda ID
     ✓ results have vote counts
     ✓ duplicate vote detected

     checks.........................: 100.00% ✓ 80790     ✗ 0
     data_received..................: 2.9 MB  5.3 kB/s
     data_sent......................: 2.9 MB  5.3 kB/s
     http_req_duration..............: avg=11.8ms   min=1.26ms  med=11.44ms max=68.87ms p(90)=16.93ms p(95)=19.69ms p(99)=27.85ms
     http_req_failed................: 0.00%   ✓ 1         ✗ 16671
     http_req_receiving.............: avg=0.08ms   min=0s      med=0s      max=2.1ms   p(90)=0s     p(95)=0s     p(99)=0s
     http_req_sending...............: avg=0.01ms   min=0s      med=0s      max=0.1ms   p(90)=0s     p(95)=0s     p(99)=0s
     http_req_waiting...............: avg=11.71ms  min=1.26ms  med=11.44ms max=68.87ms p(90)=16.93ms p(95)=19.69ms p(99)=27.85ms
     http_reqs......................: 16671   30.9/s
     iteration_duration.............: avg=1.26s    min=505.9ms med=1.25s   max=2.02s   p(90)=1.86s   p(95)=1.93s
     iterations.....................: 16661   30.8/s
     vus............................: 1       min=0       max=50
     vus_max........................: 50      min=50      max=50
```

## Coverage Results

- **Instruction Coverage**: 85.8%
- **Branch Coverage**: 63.2%

## Running Tests

```bash
# Run all tests
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
```