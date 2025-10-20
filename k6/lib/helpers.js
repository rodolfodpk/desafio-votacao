/**
 * K6 Helper Functions for Voting System Performance Tests
 */

import { check, sleep } from 'k6';
import { Rate, Counter, Trend } from 'k6/metrics';
import { generateValidCpf, generateInvalidCpf } from './cpf-generator.js';

// Custom metrics
export const voteSubmissions = new Counter('vote_submissions');
export const voteDuplicates = new Counter('vote_duplicates');
export const voteLatency = new Trend('vote_latency');
export const voteSuccessRate = new Rate('vote_success_rate');
export const rateLimitHits = new Counter('rate_limit_hits');
export const circuitBreakerOpens = new Counter('circuit_breaker_opens');
export const dbOperationLatency = new Trend('db_operation_latency');

/**
 * Create a test agenda
 */
export function createAgenda(http, baseUrl, title = 'Test Agenda', description = 'Test Description') {
  const payload = JSON.stringify({
    title: title,
    description: description
  });

  const response = http.post(`${baseUrl}/api/v1/agendas`, payload, {
    headers: { 'Content-Type': 'application/json' },
    tags: { name: 'create_agenda' }
  });

  const success = check(response, {
    'agenda created successfully': (r) => r.status === 201,
    'agenda has ID': (r) => r.json('id') !== undefined,
    'response time < 500ms': (r) => r.timings.duration < 500,
  });

  if (success) {
    return response.json('id');
  }
  
  console.error(`Failed to create agenda: ${response.status} - ${response.body}`);
  return null;
}

/**
 * Open a voting session for an agenda
 */
export function openVotingSession(http, baseUrl, agendaId, durationMinutes = 5) {
  const payload = JSON.stringify({
    durationMinutes: durationMinutes
  });

  const response = http.post(`${baseUrl}/api/v1/agendas/${agendaId}/voting-session`, payload, {
    headers: { 'Content-Type': 'application/json' },
    tags: { name: 'open_voting_session' }
  });

  const success = check(response, {
    'session opened successfully': (r) => r.status === 201,
    'session has agenda ID': (r) => r.json('agendaId') === agendaId,
    'response time < 300ms': (r) => r.timings.duration < 300,
  });

  return success;
}

/**
 * Submit a vote for an agenda
 */
export function submitVote(http, baseUrl, agendaId, cpf, voteChoice = 'Yes') {
  const payload = JSON.stringify({
    cpf: cpf,
    vote: voteChoice
  });

  const startTime = Date.now();
  const response = http.post(`${baseUrl}/api/v1/agendas/${agendaId}/votes`, payload, {
    headers: { 'Content-Type': 'application/json' },
    tags: { name: 'submit_vote' }
  });
  const endTime = Date.now();

  // Record metrics
  voteSubmissions.add(1);
  voteLatency.add(endTime - startTime);

  // Check for different response scenarios
  const checks = {
    'vote submitted successfully': (r) => r.status === 201,
    'vote has correct agenda ID': (r) => r.json('agendaId') === agendaId,
    'vote has correct CPF': (r) => r.json('cpf') === cpf,
    'vote has correct choice': (r) => r.json('vote') === voteChoice,
    'response time < 500ms': (r) => r.timings.duration < 500,
  };

  // Handle duplicate vote scenario - this is SUCCESS, not failure
  if (response.status === 400) {
    const isDuplicate = check(response, {
      'duplicate vote detected': (r) => r.json('error') === 'CPF already voted for this agenda',
    });
    
    if (isDuplicate) {
      voteDuplicates.add(1);
      // Duplicate vote rejection is correct behavior - mark as success
      voteSuccessRate.add(1);
      return { success: true, duplicate: true };
    }
  }

  // Handle rate limiting
  if (response.status === 429) {
    rateLimitHits.add(1);
    return { success: false, rateLimited: true };
  }

  // Handle circuit breaker (fast fail)
  if (response.status === 503 && response.timings.duration < 10) {
    circuitBreakerOpens.add(1);
    return { success: false, circuitBreaker: true };
  }

  const success = check(response, checks);
  voteSuccessRate.add(success ? 1 : 0);

  return { success, response };
}

/**
 * Get voting results for an agenda
 */
export function getResults(http, baseUrl, agendaId) {
  const startTime = Date.now();
  const response = http.get(`${baseUrl}/api/v1/agendas/${agendaId}/results`, {
    tags: { name: 'get_results' }
  });
  const endTime = Date.now();

  dbOperationLatency.add(endTime - startTime);

  const success = check(response, {
    'results retrieved successfully': (r) => r.status === 200,
    'results have agenda ID': (r) => r.json('agendaId') === agendaId,
    'results have vote counts': (r) => 
      typeof r.json('yesVotes') === 'number' && typeof r.json('noVotes') === 'number',
    'response time < 300ms': (r) => r.timings.duration < 300,
  });

  return { success, results: response.json() };
}

/**
 * Generate a unique CPF for a VU
 */
export function generateUniqueCpf(vuId, iteration) {
  // Generate a truly unique CPF for each VU and iteration
  // Use a combination that ensures uniqueness across all VUs and iterations
  const uniqueSeed = (vuId * 10000) + iteration;
  const timestamp = Date.now();
  
  // Create a unique 11-digit CPF-like number
  const uniqueDigits = String(timestamp).slice(-8) + String(uniqueSeed).padStart(3, '0');
  
  // Ensure it's exactly 11 digits
  return uniqueDigits.substring(0, 11);
}

/**
 * Generate a random vote choice
 */
export function getRandomVoteChoice() {
  return Math.random() < 0.6 ? 'Yes' : 'No'; // 60% Yes, 40% No
}

/**
 * Simulate realistic think time between operations
 */
export function thinkTime(minSeconds = 1, maxSeconds = 5) {
  const thinkTimeMs = (Math.random() * (maxSeconds - minSeconds) + minSeconds) * 1000;
  sleep(thinkTimeMs / 1000);
}

/**
 * Complete voting workflow: create agenda, open session, vote, get results
 */
export function completeVotingWorkflow(http, baseUrl, cpf, voteChoice = null) {
  const finalVoteChoice = voteChoice || getRandomVoteChoice();
  
  // 1. Create agenda
  const agendaId = createAgenda(http, baseUrl);
  if (!agendaId) {
    return { success: false, error: 'Failed to create agenda' };
  }

  // 2. Open voting session
  const sessionOpened = openVotingSession(http, baseUrl, agendaId);
  if (!sessionOpened) {
    return { success: false, error: 'Failed to open voting session' };
  }

  // 3. Submit vote
  const voteResult = submitVote(http, baseUrl, agendaId, cpf, finalVoteChoice);
  if (!voteResult.success) {
    return { success: false, error: 'Failed to submit vote', details: voteResult };
  }

  // 4. Get results
  const results = getResults(http, baseUrl, agendaId);
  if (!results.success) {
    return { success: false, error: 'Failed to get results' };
  }

  return { 
    success: true, 
    agendaId, 
    cpf, 
    voteChoice: finalVoteChoice,
    results: results.results 
  };
}

/**
 * Validate response structure
 */
export function validateResponse(response, expectedStatus, expectedFields = []) {
  const checks = {
    [`status is ${expectedStatus}`]: (r) => r.status === expectedStatus,
  };

  expectedFields.forEach(field => {
    checks[`has ${field} field`] = (r) => r.json(field) !== undefined;
  });

  return check(response, checks);
}

/**
 * Log test progress (only warnings and errors)
 */
export function logProgress(message, data = {}) {
  // Only log warnings and errors to reduce noise
  if (message.toLowerCase().includes('error') || message.toLowerCase().includes('warn') || message.toLowerCase().includes('failed')) {
    console.log(`[VU ${__VU}] ${message}`, JSON.stringify(data));
  }
}

/**
 * Generate test data for multiple agendas
 */
export function generateTestData(agendaCount = 10) {
  const agendas = [];
  
  for (let i = 0; i < agendaCount; i++) {
    agendas.push({
      id: null, // Will be filled after creation
      title: `Test Agenda ${i + 1}`,
      description: `Description for test agenda ${i + 1}`,
      sessionOpen: false,
      votes: []
    });
  }
  
  return agendas;
}

/**
 * Wait for a condition with timeout
 */
export function waitForCondition(condition, timeoutMs = 5000, checkIntervalMs = 100) {
  const startTime = Date.now();
  
  while (Date.now() - startTime < timeoutMs) {
    if (condition()) {
      return true;
    }
    sleep(checkIntervalMs / 1000);
  }
  
  return false;
}
