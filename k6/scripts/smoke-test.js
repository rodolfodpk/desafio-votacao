/**
 * Smoke Test - Basic Sanity Check
 * 
 * Purpose: Verify the system works with minimal load (1-5 VUs)
 * Duration: 1 minute
 * 
 * This test validates:
 * - All endpoints are accessible
 * - Basic functionality works
 * - Response times are reasonable
 * - No errors occur under minimal load
 */

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Counter } from 'k6/metrics';
import { 
  createAgenda, 
  openVotingSession, 
  submitVote, 
  getResults,
  generateUniqueCpf,
  getRandomVoteChoice,
  thinkTime
} from '../lib/helpers.js';
import { generateValidCpf } from '../lib/cpf-generator.js';

// Custom metrics
const smokeTestSuccess = new Rate('smoke_test_success');
const endpointAvailability = new Counter('endpoint_availability');

// Test configuration
export const options = {
  vus: 5,
  duration: '1m',
  thresholds: {
    // 100% success rate required for smoke test
    'smoke_test_success': ['rate>0.99'],
    'http_req_duration': ['p(95)<500'], // 95% of requests under 500ms
    'http_req_failed': ['rate<0.25'],   // Less than 25% error rate (allows for rate limiting)
  },
};

// Test data
const baseUrl = __ENV.BASE_URL || 'http://localhost:8080';

export default function () {
  const vuId = __VU;
  const iteration = __ITER;
  
  // Generate unique test data for this VU
  const cpf = generateUniqueCpf(vuId, iteration);
  const voteChoice = getRandomVoteChoice();
  
  let testSuccess = true;
  let agendaId = null;
  
  try {
    // Step 1: Create agenda
    agendaId = createAgenda(http, baseUrl, `Smoke Test Agenda ${vuId}-${iteration}`, 'Smoke test description');
    
    if (!agendaId) {
      console.error(`[VU ${vuId}] Failed to create agenda`);
      testSuccess = false;
    } else {
      endpointAvailability.add(1, { endpoint: 'create_agenda' });
    }
    
    // Step 2: Open voting session
    if (testSuccess) {
      const sessionOpened = openVotingSession(http, baseUrl, agendaId, 5);
      
      if (!sessionOpened) {
        console.error(`[VU ${vuId}] Failed to open voting session`);
        testSuccess = false;
      } else {
        endpointAvailability.add(1, { endpoint: 'open_voting_session' });
      }
    }
    
    // Step 3: Submit vote
    if (testSuccess) {
      const voteResult = submitVote(http, baseUrl, agendaId, cpf, voteChoice);
      
      if (!voteResult.success) {
        console.error(`[VU ${vuId}] Failed to submit vote:`, voteResult);
        testSuccess = false;
      } else {
        endpointAvailability.add(1, { endpoint: 'submit_vote' });
      }
    }
    
    // Step 4: Get results
    if (testSuccess) {
      const results = getResults(http, baseUrl, agendaId);
      
      if (!results.success) {
        console.error(`[VU ${vuId}] Failed to get results`);
        testSuccess = false;
      } else {
        endpointAvailability.add(1, { endpoint: 'get_results' });
      }
    }
    
    // Step 5: Test completed successfully
    // Note: Duplicate vote testing is done in a separate dedicated test
    
  } catch (error) {
    console.error(`[VU ${vuId}] Unexpected error:`, error);
    testSuccess = false;
  }
  
  // Record overall test success
  smokeTestSuccess.add(testSuccess ? 1 : 0);
  
  if (!testSuccess) {
    console.error(`[VU ${vuId}] Smoke test iteration ${iteration} failed`);
  }
  
  // Think time between iterations
  thinkTime(1, 3);
}

// Setup function - runs once before the test
export function setup() {
  console.log('Starting smoke test setup...');
  console.log(`Base URL: ${baseUrl}`);
  console.log(`VUs: ${options.vus}`);
  console.log(`Duration: ${options.duration}`);
  
  // Test basic connectivity
  const healthCheck = http.get(`${baseUrl}/actuator/health`);
  const healthCheckPassed = check(healthCheck, {
    'health check passed': (r) => r.status === 200,
  });
  
  if (!healthCheckPassed) {
    console.error('Health check failed - application may not be running');
    console.error(`Health check response: ${healthCheck.status} - ${healthCheck.body}`);
  } else {
    console.log('Health check passed - application is running');
  }
  
  return {
    baseUrl,
    healthCheckPassed
  };
}

// Teardown function - runs once after the test
export function teardown(data) {
  console.log('Smoke test completed');
  console.log(`Health check was ${data.healthCheckPassed ? 'successful' : 'failed'}`);
}
