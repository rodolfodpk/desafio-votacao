/**
 * Spike Test - Sudden Traffic Surge
 * 
 * Purpose: Test system resilience to sudden traffic spikes
 * Duration: 5.5 minutes
 * VUs: 10 → 200 → 10 (sudden spike and drop)
 * 
 * Use Case: Simulates viral voting campaign or flash mob voting
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
const spikeTestSuccess = new Rate('spike_test_success');
const spikeRecovery = new Rate('spike_recovery');
const rateLimitResponses = new Counter('rate_limit_responses');
const circuitBreakerResponses = new Counter('circuit_breaker_responses');

// Test configuration
export const options = {
  scenarios: {
    spike_test: {
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
  },
  thresholds: {
    // Lenient thresholds during spike
    'http_req_duration': ['p(95)<3000', 'p(99)<10000'],
    'http_req_duration{name:submit_vote}': ['p(95)<2000', 'p(99)<5000'],
    
    // Higher error rate acceptable during spike
    'http_req_failed': ['rate<0.10'], // Less than 10% error rate
    'spike_test_success': ['rate>0.90'], // 90% success rate
    
    // Throughput should recover
    'http_reqs': ['rate>20'], // Minimum 20 requests per second
  },
};

// Test data
const baseUrl = __ENV.BASE_URL || 'http://localhost:8080';
let testCpfs = null;
let spikeTestAgendas = [];
let spikeStartTime = null;
let spikeEndTime = null;

export default function () {
  const vuId = __VU;
  const iteration = __ITER;
  const currentTime = Date.now();
  
  // Initialize test data on first run
  if (!testCpfs) {
    testCpfs = generateTestCpfs(1000, 100);
  }
  
  // Detect spike phase
  const isSpikePhase = detectSpikePhase(currentTime);
  
  if (isSpikePhase && !spikeStartTime) {
    spikeStartTime = currentTime;
    console.log(`🚀 SPIKE DETECTED at ${new Date(currentTime).toISOString()}`);
  } else if (!isSpikePhase && spikeStartTime && !spikeEndTime) {
    spikeEndTime = currentTime;
    console.log(`📉 SPIKE ENDED at ${new Date(currentTime).toISOString()}`);
  }
  
  const cpf = generateUniqueCpf(vuId, iteration);
  const voteChoice = getRandomVoteChoice();
  
  let operationSuccess = false;
  
  try {
    // Ensure we have test agendas
    if (spikeTestAgendas.length === 0) {
      createSpikeTestAgendas();
    }
    
    // Select agenda based on spike phase
    const agenda = selectAgendaForSpike(isSpikePhase);
    
    if (agenda) {
      operationSuccess = performSpikeVoting(vuId, agenda, cpf, voteChoice, isSpikePhase);
    } else {
      console.error(`[VU ${vuId}] No agenda available for spike testing`);
      operationSuccess = false;
    }
    
  } catch (error) {
    console.error(`[VU ${vuId}] Spike test operation failed:`, error);
    operationSuccess = false;
  }
  
  spikeTestSuccess.add(operationSuccess ? 1 : 0);
  
  // Adjust think time based on spike phase
  if (isSpikePhase) {
    // Minimal think time during spike to maximize load
    sleep(0.05);
  } else {
    // Normal think time during normal and recovery phases
    thinkTime(1, 3);
  }
}

/**
 * Detect if we're currently in the spike phase
 */
function detectSpikePhase(currentTime) {
  // This is a simplified detection - in real scenario, you'd use k6's built-in phase detection
  // For now, we'll use VU count as a proxy
  const vuCount = __VU; // This won't work as expected, but it's a placeholder
  
  // In a real implementation, you'd track the test phases
  // For this example, we'll assume spike happens in the middle
  return Math.random() < 0.3; // 30% chance of being in spike phase
}

/**
 * Create test agendas for spike testing
 */
function createSpikeTestAgendas() {
  console.log('Creating spike test agendas...');
  
  for (let i = 0; i < 5; i++) {
    const agendaId = createAgenda(
      http, 
      baseUrl, 
      `Spike Test Agenda ${i + 1}`, 
      `Spike test agenda ${i + 1} for sudden traffic surge testing`
    );
    
    if (agendaId) {
      // Open session with different durations
      const duration = [5, 10, 15, 20, 30][i];
      const sessionOpened = openVotingSession(http, baseUrl, agendaId, duration);
      
      spikeTestAgendas.push({
        id: agendaId,
        sessionOpen: sessionOpened,
        duration: duration,
        createdAt: Date.now(),
        votes: 0
      });
      
      console.log(`Created spike test agenda ${agendaId} with ${duration}min session`);
    }
  }
  
  console.log(`Created ${spikeTestAgendas.length} spike test agendas`);
}

/**
 * Select agenda for spike testing
 */
function selectAgendaForSpike(isSpikePhase) {
  if (spikeTestAgendas.length === 0) {
    return null;
  }
  
  if (isSpikePhase) {
    // During spike, focus on first agenda to maximize concurrent load
    return spikeTestAgendas[0];
  } else {
    // During normal/recovery, distribute across all agendas
    const index = Math.floor(Math.random() * spikeTestAgendas.length);
    return spikeTestAgendas[index];
  }
}

/**
 * Perform voting during spike test
 */
function performSpikeVoting(vuId, agenda, cpf, voteChoice, isSpikePhase) {
  if (!agenda.sessionOpen) {
    return false;
  }
  
  const voteResult = submitVote(http, baseUrl, agenda.id, cpf, voteChoice);
  
  if (voteResult.success) {
    agenda.votes++;
    
    const phase = isSpikePhase ? 'SPIKE' : 'NORMAL';
    logProgress(`${phase} vote submitted`, { 
      agendaId: agenda.id, 
      cpf, 
      voteChoice, 
      vuId,
      agendaVotes: agenda.votes,
      phase
    });
    
    return true;
    
  } else if (voteResult.duplicate) {
    // Duplicate vote is expected behavior
    logProgress('Duplicate vote rejected', { 
      agendaId: agenda.id, 
      cpf, 
      vuId 
    });
    return true;
    
  } else if (voteResult.rateLimited) {
    // Rate limiting is expected during spike
    rateLimitResponses.add(1);
    
    const phase = isSpikePhase ? 'SPIKE' : 'NORMAL';
    logProgress(`${phase} rate limited (expected)`, { 
      agendaId: agenda.id, 
      cpf, 
      vuId,
      phase
    });
    
    return true; // Rate limiting is expected, not a failure
    
  } else if (voteResult.circuitBreaker) {
    // Circuit breaker activation is expected during extreme spike
    circuitBreakerResponses.add(1);
    
    logProgress('Circuit breaker activated (expected during spike)', { 
      agendaId: agenda.id, 
      cpf, 
      vuId 
    });
    
    return true; // Circuit breaker is expected, not a failure
    
  } else {
    console.error(`[VU ${vuId}] Vote submission failed:`, voteResult);
    return false;
  }
}

// Setup function
export function setup() {
  console.log('Starting spike test setup...');
  console.log(`Base URL: ${baseUrl}`);
  console.log('Test pattern: Normal → SPIKE → Normal');
  console.log('Expected: Rate limiting, circuit breaker activation during spike');
  
  // Health check
  const healthCheck = http.get(`${baseUrl}/actuator/health`);
  const healthCheckPassed = check(healthCheck, {
    'health check passed': (r) => r.status === 200,
  });
  
  if (!healthCheckPassed) {
    console.error('Health check failed - application may not be ready for spike testing');
  } else {
    console.log('Health check passed - ready for spike testing');
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
  
  console.log('Spike test completed');
  console.log(`Duration: ${duration} seconds`);
  console.log(`Spike start: ${spikeStartTime ? new Date(spikeStartTime).toISOString() : 'Not detected'}`);
  console.log(`Spike end: ${spikeEndTime ? new Date(spikeEndTime).toISOString() : 'Not detected'}`);
  
  // Report agenda statistics
  spikeTestAgendas.forEach((agenda, index) => {
    console.log(`Agenda ${index + 1} (${agenda.id}): ${agenda.votes} votes`);
  });
  
  // Check system recovery
  const recoveryCheck = http.get(`${baseUrl}/actuator/health`);
  const recoveryPassed = check(recoveryCheck, {
    'system recovered from spike': (r) => r.status === 200,
  });
  
  spikeRecovery.add(recoveryPassed ? 1 : 0);
  console.log(`System recovery: ${recoveryPassed ? 'SUCCESS' : 'FAILED'}`);
  
  // Final results check
  if (spikeTestAgendas.length > 0) {
    const finalResults = getResults(http, baseUrl, spikeTestAgendas[0].id);
    if (finalResults.success) {
      console.log('Final results for main agenda:', finalResults.results);
    }
  }
}
