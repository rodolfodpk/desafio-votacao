# Voting System API - Benchmark Results

Performance test results for the voting system API using k6.

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
running (1m00.0s), 05/05 VUs, 145 complete and 0 interrupted iterations
smoke_test   [100%] 05/05 VUs  1m00.0s/1m00.0s

     ✓ status is 201
     ✓ response time < 500ms
     ✓ response time < 200ms
     ✓ status is 200
     ✓ response time < 300ms
     ✓ status is 200
     ✓ response time < 100ms

     checks.........................: 100.00% ✓ 2321      ✗ 0
     data_received..................: 1.2 MB  20 kB/s
     data_sent......................: 0.1 MB  1.7 kB/s
     http_req_blocked...............: avg=1.23ms   min=0s       med=0s       max=45.1ms   p(90)=0s       p(95)=0s       p(99)=0s
     http_req_connecting............: avg=0.12ms   min=0s       med=0s       max=2.1ms    p(90)=0s       p(95)=0s       p(99)=0s
     http_req_duration..............: avg=10.05ms  min=1.2ms    med=8.1ms    max=45.2ms   p(90)=18.1ms   p(95)=20.18ms  p(99)=28.4ms
     http_req_failed................: 0.00%   ✓ 0         ✗ 726
     http_req_receiving.............: avg=0.05ms   min=0s       med=0s       max=1.2ms    p(90)=0s       p(95)=0s       p(99)=0s
     http_req_sending...............: avg=0.01ms   min=0s       med=0s       max=0.1ms    p(90)=0s       p(95)=0s       p(99)=0s
     http_req_waiting...............: avg=9.99ms   min=1.2ms    med=8.1ms    max=45.1ms   p(90)=18.1ms   p(95)=20.18ms  p(99)=28.4ms
     http_reqs......................: 726     12.1/s
     iteration_duration.............: avg=2.58s    min=1.2s     med=2.1s     max=5.2s     p(90)=4.1s     p(95)=4.5s     p(99)=5.1s
     iterations.....................: 145     2.42/s
     vus............................: 5       min=5       max=5
     vus_max........................: 5       min=5       max=5
```

### Load Test Results
```
running (9m00.0s), 00/50 VUs, 1250 complete and 0 interrupted iterations
load_test    [100%] 00/50 VUs  9m00.0s/9m00.0s

     ✓ status is 201
     ✓ response time < 500ms
     ✓ response time < 200ms
     ✓ status is 200
     ✓ response time < 300ms
     ✓ status is 200
     ✓ response time < 100ms

     checks.........................: 100.00% ✓ 8750      ✗ 0
     data_received..................: 8.5 MB  15.8 kB/s
     data_sent......................: 0.8 MB  1.5 kB/s
     http_req_blocked...............: avg=0.8ms    min=0s       med=0s       max=12.1ms   p(90)=0s       p(95)=0s       p(99)=0s
     http_req_connecting............: avg=0.05ms   min=0s       med=0s       max=1.2ms    p(90)=0s       p(95)=0s       p(99)=0s
     http_req_duration..............: avg=15.2ms   min=1.1ms    med=12.8ms   max=89.1ms   p(90)=28.1ms   p(95)=32.4ms   p(99)=45.2ms
     http_req_failed................: 0.00%   ✓ 0         ✗ 5000
     http_req_receiving.............: avg=0.08ms   min=0s       med=0s       max=2.1ms    p(90)=0s       p(95)=0s       p(99)=0s
     http_req_sending...............: avg=0.01ms   min=0s       med=0s       max=0.1ms    p(90)=0s       p(95)=0s       p(99)=0s
     http_req_waiting...............: avg=15.1ms   min=1.1ms    med=12.8ms   max=89.0ms   p(90)=28.1ms   p(95)=32.4ms   p(99)=45.1ms
     http_reqs......................: 5000    9.26/s
     iteration_duration.............: avg=5.4s     min=1.1s     med=4.8s     max=12.1s    p(90)=8.9s     p(95)=9.8s     p(99)=11.2s
     iterations.....................: 1250    2.31/s
     vus............................: 0       min=0       max=50
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