/**
 * Load Test - Normal Expected Load
 * 
 * Purpose: Test system behavior under expected normal load
 * Duration: 9 minutes (2m ramp-up + 5m steady + 2m ramp-down)
 * VUs: 0 → 50 → 0
 * 
 * Test Mix:
 * - 5% Create agendas
 * - 85% Submit votes (primary operation)
 * - 10% Get results
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
const loadTestSuccess = new Rate('load_test_success');
const operationLatency = new Trend('operation_latency');
const throughput = new Counter('throughput');

// Test configuration
export const options = {
  scenarios: {
    load_test: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '2m', target: 50 }, // Ramp up to 50 VUs
        { duration: '5m', target: 50 }, // Stay at 50 VUs
        { duration: '2m', target: 0 },  // Ramp down
      ],
    },
  },
  thresholds: {
    // Performance thresholds
    'http_req_duration': ['p(95)<1000', 'p(99)<2000'],
    'http_req_duration{name:submit_vote}': ['p(95)<500', 'p(99)<1000'],
    'http_req_duration{name:get_results}': ['p(95)<300', 'p(99)<500'],
    
    // Error rate thresholds - More realistic for load testing
    'http_req_failed': ['rate<0.05'], // Less than 5% error rate (allows for rate limiting)
    'load_test_success': ['rate>0.95'], // 95% success rate
    
    // Throughput thresholds - More realistic expectations
    'http_reqs': ['rate>50'], // Minimum 50 requests per second
  },
};

// Test data
const baseUrl = __ENV.BASE_URL || 'http://localhost:8080';
let testCpfs = null;
let sharedAgendas = [];

export default function (data) {
  const vuId = __VU;
  const iteration = __ITER;
  
  // Use agendas from setup data
  const testAgendas = data.agendas || [];
  
  // Initialize test data on first run
  if (!testCpfs) {
    testCpfs = generateTestCpfs(1000, 100);
  }
  
  // Determine operation type based on test mix
  const operationType = getOperationType();
  const cpf = generateUniqueCpf(vuId, iteration);
  const voteChoice = getRandomVoteChoice();
  
  let operationSuccess = false;
  const startTime = Date.now();
  
  try {
    switch (operationType) {
      case 'create_agenda':
        operationSuccess = performCreateAgenda(vuId, iteration);
        break;
        
      case 'submit_vote':
        operationSuccess = performSubmitVote(vuId, cpf, voteChoice, testAgendas);
        break;
        
      case 'get_results':
        operationSuccess = performGetResults(vuId, testAgendas);
        break;
        
      default:
        console.error(`[VU ${vuId}] Unknown operation type: ${operationType}`);
        operationSuccess = false;
    }
    
  } catch (error) {
    console.error(`[VU ${vuId}] Operation ${operationType} failed:`, error);
    operationSuccess = false;
  }
  
  const endTime = Date.now();
  operationLatency.add(endTime - startTime);
  loadTestSuccess.add(operationSuccess ? 1 : 0);
  throughput.add(1);
  
  // Think time between operations
  thinkTime(0.5, 2);
}

/**
 * Determine operation type based on test mix percentages
 */
function getOperationType() {
  const random = Math.random();
  
  if (random < 0.05) return 'create_agenda';      // 5%
  if (random < 0.95) return 'submit_vote';        // 85%
  return 'get_results';                           // 10%
}

/**
 * Create a new agenda
 */
function performCreateAgenda(vuId, iteration) {
  const agendaId = createAgenda(
    http, 
    baseUrl, 
    `Load Test Agenda ${vuId}-${iteration}`, 
    'Load test agenda description'
  );
  
  if (agendaId) {
    // Store agenda for other operations
    sharedAgendas.push({
      id: agendaId,
      vuId: vuId,
      sessionOpen: false,
      createdAt: Date.now()
    });
    
    // logProgress('Created agenda', { agendaId, vuId }); // Reduced logging
    return true;
  }
  
  return false;
}

/**
 * Open a voting session for an existing agenda
 */
function performOpenSession(vuId) {
  // Find an agenda without an open session
  let agenda = sharedAgendas.find(a => !a.sessionOpen);
  
  if (!agenda) {
    // Create a new agenda if none available
    const agendaId = createAgenda(http, baseUrl, `Load Test Agenda ${vuId}`, 'Load test agenda');
    if (agendaId) {
      sharedAgendas.push({
        id: agendaId,
        vuId: vuId,
        sessionOpen: false,
        createdAt: Date.now()
      });
      agenda = { id: agendaId, vuId: vuId, sessionOpen: false };
    }
  }
  
  if (agenda) {
    const sessionOpened = openVotingSession(http, baseUrl, agenda.id, 10); // 10 minute session
    
    if (sessionOpened) {
      agenda.sessionOpen = true;
      // logProgress('Opened voting session', { agendaId: agenda.id, vuId }); // Reduced logging
      return true;
    }
  }
  
  return false;
}

/**
 * Submit a vote
 */
function performSubmitVote(vuId, cpf, voteChoice, testAgendas) {
  // Find an agenda with an open session
  let agenda = testAgendas.find(a => a.sessionOpen);
  
  if (!agenda) {
    // All sessions should be pre-opened in setup
    // If no agenda with open session is found, this is an error
    console.error(`[VU ${vuId}] No agenda with open session found - all sessions should be pre-opened`);
    return false;
  }
  
  const voteResult = submitVote(http, baseUrl, agenda.id, cpf, voteChoice);
  
  if (voteResult.success) {
    // logProgress('Submitted vote', { agendaId: agenda.id, cpf, voteChoice, vuId }); // Reduced logging
    return true;
  } else if (voteResult.duplicate) {
    // Duplicate vote is expected behavior, not a failure
    logProgress('Duplicate vote rejected', { agendaId: agenda.id, cpf, vuId });
    return true;
  }
  
  return false;
}

/**
 * Get voting results
 */
function performGetResults(vuId, testAgendas) {
  // Find any agenda with results
  const agenda = testAgendas.find(a => a.sessionOpen);
  
  if (!agenda) {
    return false;
  }
  
  const results = getResults(http, baseUrl, agenda.id);
  
  if (results.success) {
    // logProgress('Retrieved results', { agendaId: agenda.id, results: results.results, vuId }); // Reduced logging
    return true;
  }
  
  return false;
}

// Setup function
export function setup() {
  console.log('Starting load test setup...');
  console.log(`Base URL: ${baseUrl}`);
  console.log('Test mix: 5% create, 85% vote, 10% results');
  
  // Pre-create some agendas for the test
  console.log('Pre-creating test agendas...');
  for (let i = 0; i < 5; i++) {
    const agendaId = createAgenda(
      http, 
      baseUrl, 
      `Pre-created Agenda ${i + 1}`, 
      'Pre-created for load testing'
    );
    
    if (agendaId) {
      sharedAgendas.push({
        id: agendaId,
        vuId: 0, // System-created
        sessionOpen: false,
        createdAt: Date.now()
      });
      
      // Open session for all agendas
      const sessionOpened = openVotingSession(http, baseUrl, agendaId, 15);
      if (sessionOpened) {
        const agenda = sharedAgendas.find(a => a.id === agendaId);
        if (agenda) agenda.sessionOpen = true;
      }
    }
  }
  
  console.log(`Pre-created ${sharedAgendas.length} agendas`);
  
  return {
    baseUrl,
    preCreatedAgendas: sharedAgendas.length,
    agendas: sharedAgendas
  };
}

// Teardown function
export function teardown(data) {
  console.log('Load test completed');
  console.log(`Pre-created ${data.preCreatedAgendas} agendas`);
  console.log(`Total agendas created during test: ${sharedAgendas.length}`);
}
