/**
 * Concurrent Voting Test - Race Condition Testing
 * 
 * Purpose: Test database consistency under high concurrency
 * Duration: 2 minutes
 * VUs: 100 concurrent users
 * 
 * Focus: Multiple VUs voting on the same agenda simultaneously
 * Validation: Vote count matches unique CPFs, no lost votes
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
  logProgress
} from '../lib/helpers.js';
import { generateTestCpfs } from '../lib/cpf-generator.js';

// Custom metrics
const concurrentTestSuccess = new Rate('concurrent_test_success');
const raceConditionDetected = new Counter('race_condition_detected');
const duplicateVoteAttempts = new Counter('duplicate_vote_attempts');
const databaseConsistency = new Rate('database_consistency');

// Test configuration
export const options = {
  scenarios: {
    concurrent_voting: {
      executor: 'constant-vus',
      vus: 100,
      duration: '2m',
    },
  },
  thresholds: {
    // Focus on consistency, not speed
    'http_req_duration': ['p(95)<2000', 'p(99)<5000'],
    'http_req_duration{name:submit_vote}': ['p(95)<1000', 'p(99)<3000'],
    
    // Error rate should be low (only duplicates expected)
    'http_req_failed': ['rate<0.02'], // Less than 2% error rate
    'concurrent_test_success': ['rate>0.98'], // 98% success rate
    
    // Database consistency is critical
    'database_consistency': ['rate>0.99'], // 99% consistency
  },
};

// Test data
const baseUrl = __ENV.BASE_URL || 'http://localhost:8080';
let testCpfs = null;
let concurrentAgenda = null;
let voteAttempts = new Map(); // Track all vote attempts
let successfulVotes = new Set(); // Track successful votes
let consistencyChecks = [];

export default function () {
  const vuId = __VU;
  const iteration = __ITER;
  
  // Initialize test data on first run
  if (!testCpfs) {
    testCpfs = generateTestCpfs(500, 50);
  }
  
  // Ensure we have the concurrent test agenda
  if (!concurrentAgenda) {
    concurrentAgenda = createConcurrentTestAgenda();
  }
  
  if (!concurrentAgenda) {
    console.error(`[VU ${vuId}] Failed to create concurrent test agenda`);
    return;
  }
  
  const cpf = generateUniqueCpf(vuId, iteration);
  const voteChoice = getRandomVoteChoice();
  
  let operationSuccess = false;
  
  try {
    // Perform concurrent voting
    operationSuccess = performConcurrentVote(vuId, cpf, voteChoice);
    
    // Occasionally check database consistency
    if (Math.random() < 0.05) { // 5% of the time
      const consistent = verifyDatabaseConsistency(vuId);
      databaseConsistency.add(consistent ? 1 : 0);
    }
    
  } catch (error) {
    console.error(`[VU ${vuId}] Concurrent voting operation failed:`, error);
    operationSuccess = false;
  }
  
  concurrentTestSuccess.add(operationSuccess ? 1 : 0);
  
  // Minimal sleep to maximize concurrency
  sleep(0.1);
}

/**
 * Create the concurrent test agenda
 */
function createConcurrentTestAgenda() {
  const agendaId = createAgenda(
    http, 
    baseUrl, 
    'CONCURRENT TEST AGENDA - Race Condition Testing', 
    'Concurrent voting test agenda for database consistency validation'
  );
  
  if (agendaId) {
    // Open a session for the test duration
    const sessionOpened = openVotingSession(http, baseUrl, agendaId, 5); // 5 minutes
    
    if (sessionOpened) {
      console.log(`Created concurrent test agenda ${agendaId} with 5-minute session`);
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
 * Perform concurrent voting
 */
function performConcurrentVote(vuId, cpf, voteChoice) {
  if (!concurrentAgenda || !concurrentAgenda.sessionOpen) {
    return false;
  }
  
  // Record vote attempt
  voteAttempts.set(cpf, {
    vuId: vuId,
    voteChoice: voteChoice,
    timestamp: Date.now(),
    attemptNumber: (voteAttempts.get(cpf)?.attemptNumber || 0) + 1
  });
  
  const voteResult = submitVote(http, baseUrl, concurrentAgenda.id, cpf, voteChoice);
  
  if (voteResult.success) {
    // Record successful vote
    successfulVotes.add(cpf);
    
    logProgress('Concurrent vote successful', { 
      agendaId: concurrentAgenda.id, 
      cpf, 
      voteChoice, 
      vuId,
      attemptNumber: voteAttempts.get(cpf).attemptNumber,
      totalSuccessful: successfulVotes.size,
      totalAttempts: voteAttempts.size
    });
    
    return true;
    
  } else if (voteResult.duplicate) {
    // Check if this is a legitimate duplicate
    const attempt = voteAttempts.get(cpf);
    if (attempt && attempt.attemptNumber > 1) {
      // This is a legitimate duplicate attempt
      duplicateVoteAttempts.add(1);
      
      logProgress('Duplicate vote correctly rejected', { 
        agendaId: concurrentAgenda.id, 
        cpf, 
        vuId,
        attemptNumber: attempt.attemptNumber
      });
      
      return true; // This is expected behavior
    } else {
      // This might be a race condition
      raceConditionDetected.add(1);
      
      console.error(`[VU ${vuId}] Potential race condition: duplicate rejection for first attempt of CPF ${cpf}`);
      return false;
    }
    
  } else {
    console.error(`[VU ${vuId}] Vote submission failed:`, voteResult);
    return false;
  }
}

/**
 * Verify database consistency
 */
function verifyDatabaseConsistency(vuId) {
  if (!concurrentAgenda) {
    return false;
  }
  
  const results = getResults(http, baseUrl, concurrentAgenda.id);
  
  if (!results.success) {
    console.error(`[VU ${vuId}] Failed to get results for consistency check`);
    return false;
  }
  
  const currentResults = results.results;
  const totalVotesInDB = currentResults.yesVotes + currentResults.noVotes;
  const totalSuccessfulVotes = successfulVotes.size;
  
  // Record consistency check
  consistencyChecks.push({
    timestamp: Date.now(),
    vuId: vuId,
    dbVoteCount: totalVotesInDB,
    successfulVoteCount: totalSuccessfulVotes,
    results: currentResults
  });
  
  // Keep only recent checks (last 20)
  if (consistencyChecks.length > 20) {
    consistencyChecks.shift();
  }
  
  // Check consistency
  const isConsistent = totalVotesInDB === totalSuccessfulVotes;
  
  if (!isConsistent) {
    console.error(`[VU ${vuId}] Database inconsistency detected:`);
    console.error(`  DB vote count: ${totalVotesInDB}`);
    console.error(`  Successful vote count: ${totalSuccessfulVotes}`);
    console.error(`  Difference: ${Math.abs(totalVotesInDB - totalSuccessfulVotes)}`);
  }
  
  logProgress('Consistency check', { 
    agendaId: concurrentAgenda.id,
    dbVoteCount: totalVotesInDB,
    successfulVoteCount: totalSuccessfulVotes,
    consistent: isConsistent,
    vuId
  });
  
  return isConsistent;
}

// Setup function
export function setup() {
  console.log('Starting concurrent voting test setup...');
  console.log(`Base URL: ${baseUrl}`);
  console.log('Focus: Database consistency under high concurrency');
  console.log('Expected: Duplicate vote rejections, no lost votes');
  
  // Health check
  const healthCheck = http.get(`${baseUrl}/actuator/health`);
  const healthCheckPassed = check(healthCheck, {
    'health check passed': (r) => r.status === 200,
  });
  
  if (!healthCheckPassed) {
    console.error('Health check failed - application may not be ready for concurrent testing');
  } else {
    console.log('Health check passed - ready for concurrent testing');
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
  
  console.log('Concurrent voting test completed');
  console.log(`Duration: ${duration} seconds`);
  console.log(`Total vote attempts: ${voteAttempts.size}`);
  console.log(`Total successful votes: ${successfulVotes.size}`);
  console.log(`Duplicate vote attempts: ${duplicateVoteAttempts.count}`);
  console.log(`Race conditions detected: ${raceConditionDetected.count}`);
  console.log(`Consistency checks performed: ${consistencyChecks.length}`);
  
  // Final consistency check
  if (concurrentAgenda) {
    const finalResults = getResults(http, baseUrl, concurrentAgenda.id);
    if (finalResults.success) {
      const totalVotesInDB = finalResults.results.yesVotes + finalResults.results.noVotes;
      const totalSuccessfulVotes = successfulVotes.size;
      
      console.log('Final consistency check:');
      console.log(`  DB vote count: ${totalVotesInDB}`);
      console.log(`  Successful vote count: ${totalSuccessfulVotes}`);
      console.log(`  Consistent: ${totalVotesInDB === totalSuccessfulVotes ? 'YES' : 'NO'}`);
      console.log('Final results:', finalResults.results);
      
      // Report on vote distribution
      const yesVotes = finalResults.results.yesVotes;
      const noVotes = finalResults.results.noVotes;
      const totalVotes = yesVotes + noVotes;
      
      if (totalVotes > 0) {
        console.log(`Vote distribution: ${((yesVotes / totalVotes) * 100).toFixed(1)}% Yes, ${((noVotes / totalVotes) * 100).toFixed(1)}% No`);
      }
    }
  }
  
  // Report consistency check history
  if (consistencyChecks.length > 0) {
    const consistentChecks = consistencyChecks.filter(check => 
      check.dbVoteCount === check.successfulVoteCount
    ).length;
    
    console.log(`Consistency check success rate: ${((consistentChecks / consistencyChecks.length) * 100).toFixed(1)}%`);
  }
}
