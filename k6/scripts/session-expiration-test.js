/**
 * Session Expiration Test - Test voting on expired sessions
 * 
 * Purpose: Test that the system correctly rejects votes on expired sessions
 * Duration: 2 minutes
 * 
 * This test validates:
 * - Votes are rejected when submitted after session expiration
 * - Correct error messages are returned for expired sessions
 * - System remains stable when handling expired session requests
 * - Performance is maintained during expiration handling
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
const expirationTestSuccess = new Rate('expiration_test_success');
const expiredSessionRejections = new Counter('expired_session_rejections');
const expirationErrors = new Counter('expiration_errors');

// Test configuration
export const options = {
  vus: 2,
  duration: '2m',
  thresholds: {
    // 100% success rate required for expiration test
    'expiration_test_success': ['rate>0.99'],
    'http_req_duration': ['p(95)<500'], // 95% of requests under 500ms
    'http_req_failed': ['rate<0.01'],   // Less than 1% error rate (only real errors)
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
    agendaId = createAgenda(http, baseUrl, `Expiration Test Agenda ${vuId}-${iteration}`, 'Session expiration test description');
    
    if (!agendaId) {
      console.error(`[VU ${vuId}] Failed to create agenda`);
      testSuccess = false;
    }
    
    // Step 2: Open voting session with very short duration (1 minute)
    if (testSuccess) {
      const sessionOpened = openVotingSession(http, baseUrl, agendaId, 1); // 1 minute only
      
      if (!sessionOpened) {
        console.error(`[VU ${vuId}] Failed to open voting session`);
        testSuccess = false;
      }
    }
    
    // Step 3: Wait for session to expire (70 seconds)
    if (testSuccess) {
      // Only log occasionally to reduce noise
      if (iteration % 5 === 0) {
        console.log(`[VU ${vuId}] Waiting for session to expire...`);
      }
      sleep(70); // Wait longer than 1 minute session duration
    }
    
    // Step 4: Try to vote on expired session (should be rejected)
    if (testSuccess) {
      const expiredVoteResult = submitVote(http, baseUrl, agendaId, cpf, voteChoice);
      
      if (expiredVoteResult.success) {
        console.error(`[VU ${vuId}] Vote on expired session should have been rejected but wasn't`);
        testSuccess = false;
        expirationErrors.add(1);
      } else {
        // Check if it's the expected expiration error
        const isExpired = check(expiredVoteResult.response, {
          'expired session detected': (r) => r.status === 400 && r.json('error') === 'Voting session is closed',
        });
        
        if (isExpired) {
          expiredSessionRejections.add(1);
          // Only log occasionally to reduce noise
          if (iteration % 5 === 0) {
            console.log(`[VU ${vuId}] Expired session correctly rejected for CPF ${cpf}`);
          }
        } else {
          console.error(`[VU ${vuId}] Unexpected expired session response:`, expiredVoteResult);
          testSuccess = false;
          expirationErrors.add(1);
        }
      }
    }
    
    // Step 5: Try to vote again on same expired session (should still be rejected)
    if (testSuccess) {
      const secondExpiredVoteResult = submitVote(http, baseUrl, agendaId, cpf, voteChoice);
      
      if (secondExpiredVoteResult.success) {
        console.error(`[VU ${vuId}] Second vote on expired session should have been rejected`);
        testSuccess = false;
        expirationErrors.add(1);
      } else {
        // Check if it's the expected expiration error
        const isExpired = check(secondExpiredVoteResult.response, {
          'second expired session detected': (r) => r.status === 400 && r.json('error') === 'Voting session is closed',
        });
        
        if (isExpired) {
          expiredSessionRejections.add(1);
          // Only log occasionally to reduce noise
          if (iteration % 10 === 0) {
            console.log(`[VU ${vuId}] Second expired session vote correctly rejected`);
          }
        } else {
          console.error(`[VU ${vuId}] Second vote unexpected response:`, secondExpiredVoteResult);
          testSuccess = false;
          expirationErrors.add(1);
        }
      }
    }
    
  } catch (error) {
    console.error(`[VU ${vuId}] Unexpected error:`, error);
    testSuccess = false;
  }
  
  // Record overall test success
  expirationTestSuccess.add(testSuccess ? 1 : 0);
  
  if (!testSuccess) {
    console.error(`[VU ${vuId}] Session expiration test iteration ${iteration} failed`);
  }
  
  // Think time between iterations
  thinkTime(2, 4);
}

// Setup function - runs once before the test
export function setup() {
  console.log('Starting session expiration test setup...');
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
  console.log('Session expiration test completed');
  console.log(`Health check was ${data.healthCheckPassed ? 'successful' : 'failed'}`);
}
