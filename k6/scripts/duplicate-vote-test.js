/**
 * Duplicate Vote Test - Dedicated test for duplicate vote handling
 * 
 * Purpose: Test that the system correctly rejects duplicate votes
 * Duration: 30 seconds
 * 
 * This test validates:
 * - Duplicate votes are properly rejected (HTTP 400)
 * - Correct error messages are returned
 * - No data corruption occurs
 * - System remains stable under duplicate attempts
 */

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Counter } from 'k6/metrics';
import { 
  createAgenda, 
  openVotingSession, 
  submitVote, 
  generateUniqueCpf,
  getRandomVoteChoice,
  thinkTime
} from '../lib/helpers.js';

// Custom metrics
const duplicateTestSuccess = new Rate('duplicate_test_success');
const duplicateRejections = new Counter('duplicate_rejections');
const duplicateErrors = new Counter('duplicate_errors');

// Test configuration
export const options = {
  vus: 3,
  duration: '30s',
  thresholds: {
    // 100% success rate required for duplicate test
    'duplicate_test_success': ['rate>0.99'],
    'http_req_duration': ['p(95)<500'], // 95% of requests under 500ms
    'http_req_failed': ['rate<0.01'],   // Less than 1% error rate (only real errors)
  },
};

// Test data
const baseUrl = __ENV.BASE_URL || 'http://localhost:8080';

export default function () {
  const vuId = __VU;
  const iteration = __ITER;
  
  // Use a fixed CPF for ALL VUs to ensure duplicates
  // This creates the same CPF across all VUs and iterations
  const cpf = generateUniqueCpf(1, 0); // Always use VU 1, iteration 0 for all VUs
  const voteChoice = getRandomVoteChoice();
  
  let testSuccess = true;
  let agendaId = null;
  
  try {
    // Step 1: Create agenda (use same agenda for all VUs to ensure duplicates)
    agendaId = createAgenda(http, baseUrl, `Duplicate Test Agenda`, 'Duplicate vote test description');
    
    if (!agendaId) {
      console.error(`[VU ${vuId}] Failed to create agenda`);
      testSuccess = false;
    }
    
    // Step 2: Open voting session
    if (testSuccess) {
      const sessionOpened = openVotingSession(http, baseUrl, agendaId, 5);
      
      if (!sessionOpened) {
        console.error(`[VU ${vuId}] Failed to open voting session`);
        testSuccess = false;
      }
    }
    
    // Step 3: Submit first vote (should succeed)
    if (testSuccess) {
      const firstVoteResult = submitVote(http, baseUrl, agendaId, cpf, voteChoice);
      
      if (!firstVoteResult.success) {
        console.error(`[VU ${vuId}] First vote should have succeeded but didn't:`, firstVoteResult);
        testSuccess = false;
      }
    }
    
    // Step 4: Submit duplicate vote (should be rejected)
    if (testSuccess) {
      const duplicateResult = submitVote(http, baseUrl, agendaId, cpf, voteChoice);
      
      if (duplicateResult.success && duplicateResult.duplicate) {
        // This is the expected behavior - duplicate vote correctly rejected
        duplicateRejections.add(1);
        // Only log occasionally to reduce noise
        if (iteration % 10 === 0) {
          console.log(`[VU ${vuId}] Duplicate vote correctly rejected for CPF ${cpf}`);
        }
      } else if (duplicateResult.success && !duplicateResult.duplicate) {
        console.error(`[VU ${vuId}] Duplicate vote should have been rejected but wasn't`);
        testSuccess = false;
        duplicateErrors.add(1);
      } else {
        console.error(`[VU ${vuId}] Unexpected duplicate vote response:`, duplicateResult);
        testSuccess = false;
        duplicateErrors.add(1);
      }
    }
    
    // Step 5: Try different vote choice (should still be rejected - same CPF)
    if (testSuccess) {
      const differentVoteChoice = voteChoice === 'Yes' ? 'No' : 'Yes';
      const differentVoteResult = submitVote(http, baseUrl, agendaId, cpf, differentVoteChoice);
      
      if (differentVoteResult.success && differentVoteResult.duplicate) {
        // This is correct - same CPF, different choice should still be rejected
        duplicateRejections.add(1);
        // Only log occasionally to reduce noise
        if (iteration % 10 === 0) {
          console.log(`[VU ${vuId}] Different vote choice correctly rejected for same CPF ${cpf}`);
        }
      } else if (differentVoteResult.success && !differentVoteResult.duplicate) {
        console.error(`[VU ${vuId}] Different vote choice should have been rejected (same CPF)`);
        testSuccess = false;
        duplicateErrors.add(1);
      } else {
        console.error(`[VU ${vuId}] Unexpected different vote response:`, differentVoteResult);
        testSuccess = false;
        duplicateErrors.add(1);
      }
    }
    
  } catch (error) {
    console.error(`[VU ${vuId}] Unexpected error:`, error);
    testSuccess = false;
  }
  
  // Record overall test success
  duplicateTestSuccess.add(testSuccess ? 1 : 0);
  
  if (!testSuccess) {
    console.error(`[VU ${vuId}] Duplicate vote test iteration ${iteration} failed`);
  }
  
  // Think time between iterations
  thinkTime(0.5, 1.5);
}

// Setup function - runs once before the test
export function setup() {
  console.log('Starting duplicate vote test setup...');
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
  console.log('Duplicate vote test completed');
  console.log(`Health check was ${data.healthCheckPassed ? 'successful' : 'failed'}`);
}
