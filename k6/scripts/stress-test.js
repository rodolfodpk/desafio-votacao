/**
 * Stress Test - Find Breaking Point
 * 
 * Purpose: Find the system's breaking point under high load
 * Duration: 14 minutes (5m ramp-up + 5m high load + 2m peak + 2m cool-down)
 * VUs: 0 → 200 → 300 → 0
 * 
 * Focus: Concurrent voting on same agenda to test database consistency
 */

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Counter, Trend } from 'k6/metrics';
import { 
  createAgenda, 
  openVotingSession, 
  submitVote, 
  getResults,
  generateUniqueCpf,
  getRandomVoteChoice,
  thinkTime,
  logProgress
} from '../lib/helpers.js';
import { generateTestCpfs } from '../lib/cpf-generator.js';

// Custom metrics
const stressTestSuccess = new Rate('stress_test_success');
const concurrentVotes = new Counter('concurrent_votes');
const databaseConsistency = new Rate('database_consistency');
const systemRecovery = new Rate('system_recovery');

// Test configuration
export const options = {
  scenarios: {
    stress_test: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '5m', target: 200 }, // Ramp up to 200 VUs
        { duration: '5m', target: 200 }, // Stay at 200 VUs
        { duration: '2m', target: 300 }, // Peak at 300 VUs
        { duration: '2m', target: 0 },   // Cool down
      ],
    },
  },
  thresholds: {
    // More lenient thresholds for stress test
    'http_req_duration': ['p(95)<2000', 'p(99)<5000'],
    'http_req_duration{name:submit_vote}': ['p(95)<1000', 'p(99)<3000'],
    
    // Higher error rate acceptable during stress
    'http_req_failed': ['rate<0.05'], // Less than 5% error rate
    'stress_test_success': ['rate>0.95'], // 95% success rate
    
    // Throughput should still be reasonable
    'http_reqs': ['rate>50'], // Minimum 50 requests per second
  },
};

// Test data
const baseUrl = __ENV.BASE_URL || 'http://localhost:8080';
let testCpfs = null;
let stressTestAgenda = null;
let voteCounts = new Map(); // Track votes per CPF
let resultsHistory = [];

export default function () {
  const vuId = __VU;
  const iteration = __ITER;
  
  // Initialize test data on first run
  if (!testCpfs) {
    testCpfs = generateTestCpfs(2000, 200);
  }
  
  // Ensure we have a stress test agenda
  if (!stressTestAgenda) {
    stressTestAgenda = createStressTestAgenda();
  }
  
  if (!stressTestAgenda) {
    console.error(`[VU ${vuId}] Failed to create stress test agenda`);
    return;
  }
  
  const cpf = generateUniqueCpf(vuId, iteration);
  const voteChoice = getRandomVoteChoice();
  
  let operationSuccess = false;
  
  try {
    // Focus on concurrent voting to stress the database
    operationSuccess = performConcurrentVoting(vuId, cpf, voteChoice);
    
    // Occasionally check results to verify consistency
    if (Math.random() < 0.1) { // 10% of the time
      const resultsConsistent = verifyResultsConsistency(vuId);
      databaseConsistency.add(resultsConsistent ? 1 : 0);
    }
    
  } catch (error) {
    console.error(`[VU ${vuId}] Stress test operation failed:`, error);
    operationSuccess = false;
  }
  
  stressTestSuccess.add(operationSuccess ? 1 : 0);
  concurrentVotes.add(1);
  
  // Minimal think time to maximize stress
  sleep(0.1);
}

/**
 * Create the main stress test agenda
 */
function createStressTestAgenda() {
  const agendaId = createAgenda(
    http, 
    baseUrl, 
    'STRESS TEST AGENDA - High Concurrency', 
    'Stress test agenda for concurrent voting validation'
  );
  
  if (agendaId) {
    // Open a long session for stress testing
    const sessionOpened = openVotingSession(http, baseUrl, agendaId, 30); // 30 minutes
    
    if (sessionOpened) {
      console.log(`Created stress test agenda ${agendaId} with 30-minute session`);
      return {
        id: agendaId,
        sessionOpen: true,
        createdAt: Date.now()
      };
    }
  }
  
  return null;
}

/**
 * Perform concurrent voting to stress the database
 */
function performConcurrentVoting(vuId, cpf, voteChoice) {
  if (!stressTestAgenda || !stressTestAgenda.sessionOpen) {
    return false;
  }
  
  const voteResult = submitVote(http, baseUrl, stressTestAgenda.id, cpf, voteChoice);
  
  if (voteResult.success) {
    // Track successful votes
    voteCounts.set(cpf, {
      vote: voteChoice,
      timestamp: Date.now(),
      vuId: vuId
    });
    
    logProgress('Concurrent vote submitted', { 
      agendaId: stressTestAgenda.id, 
      cpf, 
      voteChoice, 
      vuId,
      totalVotes: voteCounts.size
    });
    
    return true;
    
  } else if (voteResult.duplicate) {
    // Verify this is actually a duplicate
    const existingVote = voteCounts.get(cpf);
    if (existingVote) {
      logProgress('Duplicate vote correctly rejected', { 
        agendaId: stressTestAgenda.id, 
        cpf, 
        vuId 
      });
      return true; // This is expected behavior
    } else {
      console.error(`[VU ${vuId}] Unexpected duplicate rejection for new CPF: ${cpf}`);
      return false;
    }
    
  } else if (voteResult.rateLimited) {
    // Rate limiting is expected under high stress
    logProgress('Rate limited (expected under stress)', { 
      agendaId: stressTestAgenda.id, 
      cpf, 
      vuId 
    });
    return true; // Not a failure, just rate limiting
    
  } else if (voteResult.circuitBreaker) {
    // Circuit breaker activation is expected under extreme stress
    logProgress('Circuit breaker activated (expected under extreme stress)', { 
      agendaId: stressTestAgenda.id, 
      cpf, 
      vuId 
    });
    return true; // Not a failure, just circuit protection
    
  } else {
    console.error(`[VU ${vuId}] Vote submission failed:`, voteResult);
    return false;
  }
}

/**
 * Verify database consistency by checking results
 */
function verifyResultsConsistency(vuId) {
  if (!stressTestAgenda) {
    return false;
  }
  
  const results = getResults(http, baseUrl, stressTestAgenda.id);
  
  if (!results.success) {
    console.error(`[VU ${vuId}] Failed to get results for consistency check`);
    return false;
  }
  
  const currentResults = results.results;
  resultsHistory.push({
    timestamp: Date.now(),
    results: currentResults,
    vuId: vuId
  });
  
  // Keep only recent results (last 10)
  if (resultsHistory.length > 10) {
    resultsHistory.shift();
  }
  
  // Check if results are consistent (should only increase)
  if (resultsHistory.length > 1) {
    const previousResults = resultsHistory[resultsHistory.length - 2];
    const totalPrevious = previousResults.results.yesVotes + previousResults.results.noVotes;
    const totalCurrent = currentResults.yesVotes + currentResults.noVotes;
    
    if (totalCurrent < totalPrevious) {
      console.error(`[VU ${vuId}] Database inconsistency detected: vote count decreased from ${totalPrevious} to ${totalCurrent}`);
      return false;
    }
  }
  
  logProgress('Results consistency verified', { 
    agendaId: stressTestAgenda.id,
    results: currentResults,
    vuId 
  });
  
  return true;
}

// Setup function
export function setup() {
  console.log('Starting stress test setup...');
  console.log(`Base URL: ${baseUrl}`);
  console.log('Focus: Concurrent voting on single agenda');
  console.log('Expected: Rate limiting, circuit breaker activation');
  
  // Health check
  const healthCheck = http.get(`${baseUrl}/actuator/health`);
  const healthCheckPassed = check(healthCheck, {
    'health check passed': (r) => r.status === 200,
  });
  
  if (!healthCheckPassed) {
    console.error('Health check failed - application may not be ready for stress testing');
  } else {
    console.log('Health check passed - ready for stress testing');
  }
  
  return {
    baseUrl,
    healthCheckPassed,
    startTime: Date.now()
  };
}

// Teardown function
export function teardown(data) {
  const endTime = Date.now();
  const duration = (endTime - data.startTime) / 1000;
  
  console.log('Stress test completed');
  console.log(`Duration: ${duration} seconds`);
  console.log(`Total unique votes attempted: ${voteCounts.size}`);
  console.log(`Results history entries: ${resultsHistory.length}`);
  
  // Final consistency check
  if (stressTestAgenda) {
    const finalResults = getResults(http, baseUrl, stressTestAgenda.id);
    if (finalResults.success) {
      console.log('Final results:', finalResults.results);
    }
  }
  
  // Check system recovery
  const recoveryCheck = http.get(`${baseUrl}/actuator/health`);
  const recoveryPassed = check(recoveryCheck, {
    'system recovered': (r) => r.status === 200,
  });
  
  systemRecovery.add(recoveryPassed ? 1 : 0);
  console.log(`System recovery: ${recoveryPassed ? 'SUCCESS' : 'FAILED'}`);
}
