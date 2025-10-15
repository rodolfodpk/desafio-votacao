/**
 * K6 Configuration for Voting System Performance Tests
 */

// Environment configuration
export const config = {
  // Base URL for the voting system API
  baseUrl: __ENV.BASE_URL || 'http://localhost:8080',
  
  // Test data configuration
  testData: {
    // Number of test agendas to create
    agendaCount: parseInt(__ENV.AGENDA_COUNT) || 10,
    
    // Number of unique CPFs to generate
    cpfCount: parseInt(__ENV.CPF_COUNT) || 1000,
    
    // Vote distribution (percentage)
    voteDistribution: {
      yes: parseFloat(__ENV.YES_PERCENTAGE) || 60.0,
      no: parseFloat(__ENV.NO_PERCENTAGE) || 40.0
    }
  },
  
  // Performance thresholds
  thresholds: {
    // Response time thresholds
    http_req_duration: ['p(95)<1000', 'p(99)<2000'],
    
    // Vote submission specific thresholds
    'http_req_duration{name:submit_vote}': ['p(95)<500', 'p(99)<1000'],
    
    // Results retrieval thresholds
    'http_req_duration{name:get_results}': ['p(95)<300', 'p(99)<500'],
    
    // Error rate thresholds
    http_req_failed: ['rate<0.01'], // Less than 1% error rate
    
    // Throughput thresholds
    http_reqs: ['rate>100'], // Minimum 100 requests per second
    
    // Custom metrics
    vote_success_rate: ['rate>0.999'], // 99.9% success rate
    duplicate_vote_rate: ['rate<0.001'], // Less than 0.1% duplicate attempts
  },
  
  // Test scenarios configuration
  scenarios: {
    // Smoke test - basic functionality
    smoke: {
      executor: 'constant-vus',
      vus: 5,
      duration: '1m',
    },
    
    // Load test - normal expected load
    load: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '2m', target: 50 }, // Ramp up to 50 VUs
        { duration: '5m', target: 50 }, // Stay at 50 VUs
        { duration: '2m', target: 0 },  // Ramp down
      ],
    },
    
    // Stress test - find breaking point
    stress: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '5m', target: 200 }, // Ramp up to 200 VUs
        { duration: '5m', target: 200 }, // Stay at 200 VUs
        { duration: '2m', target: 300 }, // Peak at 300 VUs
        { duration: '2m', target: 0 },   // Cool down
      ],
    },
    
    // Spike test - sudden traffic surge
    spike: {
      executor: 'ramping-vus',
      startVUs: 10,
      stages: [
        { duration: '2m', target: 10 },  // Normal load
        { duration: '10s', target: 200 }, // Sudden spike
        { duration: '1m', target: 200 },  // Sustain spike
        { duration: '10s', target: 10 },  // Drop back
        { duration: '2m', target: 10 },   // Recovery
      ],
    },
    
    // Soak test - endurance testing
    soak: {
      executor: 'constant-vus',
      vus: 30,
      duration: '4h',
    },
    
    // Concurrent voting - race condition testing
    concurrent: {
      executor: 'constant-vus',
      vus: 100,
      duration: '2m',
    }
  }
};

// API endpoints
export const endpoints = {
  agendas: '/api/agendas',
  votingSession: (agendaId) => `/api/agendas/${agendaId}/voting-session`,
  votes: (agendaId) => `/api/agendas/${agendaId}/votes`,
  results: (agendaId) => `/api/agendas/${agendaId}/results`,
  cpfValidation: '/api/cpf-validation'
};

// HTTP client configuration
export const httpConfig = {
  timeout: '30s',
  headers: {
    'Content-Type': 'application/json',
    'Accept': 'application/json'
  }
};

// Test data templates
export const templates = {
  agenda: {
    title: 'Test Agenda',
    description: 'Test Description for Performance Testing'
  },
  
  votingSession: {
    durationMinutes: 5
  },
  
  vote: {
    cpf: '', // Will be filled dynamically
    vote: 'Yes' // Will be varied
  }
};

// Performance metrics to track
export const metrics = {
  // Custom metrics for business logic
  voteSubmissions: new Counter('vote_submissions'),
  voteDuplicates: new Counter('vote_duplicates'),
  voteLatency: new Trend('vote_latency'),
  voteSuccessRate: new Rate('vote_success_rate'),
  
  // Circuit breaker metrics
  circuitBreakerOpens: new Counter('circuit_breaker_opens'),
  circuitBreakerCalls: new Counter('circuit_breaker_calls'),
  
  // Rate limiter metrics
  rateLimitHits: new Counter('rate_limit_hits'),
  
  // Database metrics
  dbOperationLatency: new Trend('db_operation_latency'),
  dbConnectionPoolExhaustion: new Counter('db_connection_pool_exhaustion')
};
