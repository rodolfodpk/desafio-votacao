/**
 * Mixed Workload Test - Realistic Production Simulation
 * 
 * Purpose: Simulate realistic production patterns
 * Duration: 10 minutes
 * VUs: 30 steady users
 * 
 * Test Mix:
 * - 70% voting on hot agendas (high traffic)
 * - 20% voting on other agendas (medium traffic)
 * - 10% checking results (read operations)
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
const mixedWorkloadSuccess = new Rate('mixed_workload_success');
const hotAgendaVotes = new Counter('hot_agenda_votes');
const mediumAgendaVotes = new Counter('medium_agenda_votes');
const resultChecks = new Counter('result_checks');

// Test configuration
export const options = {
  scenarios: {
    mixed_workload: {
      executor: 'constant-vus',
      vus: 30,
      duration: '10m',
    },
  },
  thresholds: {
    // Realistic production thresholds
    'http_req_duration': ['p(95)<1000', 'p(99)<2000'],
    'http_req_duration{name:submit_vote}': ['p(95)<500', 'p(99)<1000'],
    'http_req_duration{name:get_results}': ['p(95)<300', 'p(99)<500'],
    
    // Error rate thresholds
    'http_req_failed': ['rate<0.01'], // Less than 1% error rate
    'mixed_workload_success': ['rate>0.99'], // 99% success rate
    
    // Throughput thresholds
    'http_reqs': ['rate>50'], // Minimum 50 requests per second
  },
};

// Test data
const baseUrl = __ENV.BASE_URL || 'http://localhost:8080';
let testCpfs = null;
let testAgendas = [];
let hotAgendas = []; // High traffic agendas
let mediumAgendas = []; // Medium traffic agendas

export default function () {
  const vuId = __VU;
  const iteration = __ITER;
  
  // Initialize test data on first run
  if (!testCpfs) {
    testCpfs = generateTestCpfs(1000, 100);
  }
  
  // Ensure we have test agendas
  if (testAgendas.length === 0) {
    createMixedWorkloadAgendas();
  }
  
  const cpf = generateUniqueCpf(vuId, iteration);
  const voteChoice = getRandomVoteChoice();
  
  // Determine operation type based on realistic mix
  const operationType = getRealisticOperationType();
  
  let operationSuccess = false;
  
  try {
    switch (operationType) {
      case 'hot_agenda_vote':
        operationSuccess = performHotAgendaVoting(vuId, cpf, voteChoice);
        break;
        
      case 'medium_agenda_vote':
        operationSuccess = performMediumAgendaVoting(vuId, cpf, voteChoice);
        break;
        
      case 'check_results':
        operationSuccess = performResultCheck(vuId);
        break;
        
      default:
        console.error(`[VU ${vuId}] Unknown operation type: ${operationType}`);
        operationSuccess = false;
    }
    
  } catch (error) {
    console.error(`[VU ${vuId}] Mixed workload operation failed:`, error);
    operationSuccess = false;
  }
  
  mixedWorkloadSuccess.add(operationSuccess ? 1 : 0);
  
  // Realistic think time between operations
  thinkTime(2, 8);
}

/**
 * Determine operation type based on realistic production mix
 */
function getRealisticOperationType() {
  const random = Math.random();
  
  if (random < 0.70) return 'hot_agenda_vote';    // 70% - hot agenda voting
  if (random < 0.90) return 'medium_agenda_vote'; // 20% - medium agenda voting
  return 'check_results';                         // 10% - result checking
}

/**
 * Create mixed workload test agendas
 */
function createMixedWorkloadAgendas() {
  console.log('Creating mixed workload test agendas...');
  
  // Create hot agendas (high traffic)
  for (let i = 0; i < 3; i++) {
    const agendaId = createAgenda(
      http, 
      baseUrl, 
      `🔥 HOT Agenda ${i + 1} - High Traffic`, 
      `Hot agenda ${i + 1} for high traffic simulation`
    );
    
    if (agendaId) {
      const sessionOpened = openVotingSession(http, baseUrl, agendaId, 15);
      
      const agenda = {
        id: agendaId,
        title: `🔥 HOT Agenda ${i + 1}`,
        sessionOpen: sessionOpened,
        traffic: 'hot',
        votes: 0,
        createdAt: Date.now()
      };
      
      testAgendas.push(agenda);
      hotAgendas.push(agenda);
      
      console.log(`Created hot agenda ${agendaId}`);
    }
  }
  
  // Create medium agendas (medium traffic)
  for (let i = 0; i < 5; i++) {
    const agendaId = createAgenda(
      http, 
      baseUrl, 
      `📊 Medium Agenda ${i + 1} - Medium Traffic`, 
      `Medium agenda ${i + 1} for medium traffic simulation`
    );
    
    if (agendaId) {
      const sessionOpened = openVotingSession(http, baseUrl, agendaId, 20);
      
      const agenda = {
        id: agendaId,
        title: `📊 Medium Agenda ${i + 1}`,
        sessionOpen: sessionOpened,
        traffic: 'medium',
        votes: 0,
        createdAt: Date.now()
      };
      
      testAgendas.push(agenda);
      mediumAgendas.push(agenda);
      
      console.log(`Created medium agenda ${agendaId}`);
    }
  }
  
  console.log(`Created ${testAgendas.length} test agendas (${hotAgendas.length} hot, ${mediumAgendas.length} medium)`);
}

/**
 * Perform voting on hot agendas (70% of traffic)
 */
function performHotAgendaVoting(vuId, cpf, voteChoice) {
  if (hotAgendas.length === 0) {
    return false;
  }
  
  // Select hot agenda (weighted towards first agenda to simulate viral content)
  const agendaIndex = Math.random() < 0.6 ? 0 : Math.floor(Math.random() * hotAgendas.length);
  const agenda = hotAgendas[agendaIndex];
  
  if (!agenda.sessionOpen) {
    return false;
  }
  
  const voteResult = submitVote(http, baseUrl, agenda.id, cpf, voteChoice);
  
  if (voteResult.success) {
    agenda.votes++;
    hotAgendaVotes.add(1);
    
    logProgress('Hot agenda vote', { 
      agendaId: agenda.id,
      agendaTitle: agenda.title,
      cpf, 
      voteChoice, 
      vuId,
      agendaVotes: agenda.votes,
      traffic: 'hot'
    });
    
    return true;
    
  } else if (voteResult.duplicate) {
    logProgress('Hot agenda duplicate vote', { 
      agendaId: agenda.id,
      cpf, 
      vuId 
    });
    return true; // Expected behavior
    
  } else {
    console.error(`[VU ${vuId}] Hot agenda vote failed:`, voteResult);
    return false;
  }
}

/**
 * Perform voting on medium agendas (20% of traffic)
 */
function performMediumAgendaVoting(vuId, cpf, voteChoice) {
  if (mediumAgendas.length === 0) {
    return false;
  }
  
  // Select medium agenda randomly
  const agendaIndex = Math.floor(Math.random() * mediumAgendas.length);
  const agenda = mediumAgendas[agendaIndex];
  
  if (!agenda.sessionOpen) {
    return false;
  }
  
  const voteResult = submitVote(http, baseUrl, agenda.id, cpf, voteChoice);
  
  if (voteResult.success) {
    agenda.votes++;
    mediumAgendaVotes.add(1);
    
    logProgress('Medium agenda vote', { 
      agendaId: agenda.id,
      agendaTitle: agenda.title,
      cpf, 
      voteChoice, 
      vuId,
      agendaVotes: agenda.votes,
      traffic: 'medium'
    });
    
    return true;
    
  } else if (voteResult.duplicate) {
    logProgress('Medium agenda duplicate vote', { 
      agendaId: agenda.id,
      cpf, 
      vuId 
    });
    return true; // Expected behavior
    
  } else {
    console.error(`[VU ${vuId}] Medium agenda vote failed:`, voteResult);
    return false;
  }
}

/**
 * Perform result checking (10% of traffic)
 */
function performResultCheck(vuId) {
  if (testAgendas.length === 0) {
    return false;
  }
  
  // Select agenda for result checking (prefer hot agendas)
  const agenda = Math.random() < 0.7 ? 
    hotAgendas[Math.floor(Math.random() * hotAgendas.length)] :
    mediumAgendas[Math.floor(Math.random() * mediumAgendas.length)];
  
  if (!agenda) {
    return false;
  }
  
  const results = getResults(http, baseUrl, agenda.id);
  
  if (results.success) {
    resultChecks.add(1);
    
    logProgress('Result check', { 
      agendaId: agenda.id,
      agendaTitle: agenda.title,
      results: results.results,
      vuId,
      traffic: agenda.traffic
    });
    
    return true;
  }
  
  return false;
}

// Setup function
export function setup() {
  console.log('Starting mixed workload test setup...');
  console.log(`Base URL: ${baseUrl}`);
  console.log('Test mix: 70% hot agenda votes, 20% medium agenda votes, 10% result checks');
  console.log('Simulating realistic production traffic patterns');
  
  // Health check
  const healthCheck = http.get(`${baseUrl}/actuator/health`);
  const healthCheckPassed = check(healthCheck, {
    'health check passed': (r) => r.status === 200,
  });
  
  if (!healthCheckPassed) {
    console.error('Health check failed - application may not be ready for mixed workload testing');
  } else {
    console.log('Health check passed - ready for mixed workload testing');
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
  
  console.log('Mixed workload test completed');
  console.log(`Duration: ${duration} seconds`);
  
  // Report agenda statistics
  console.log('\n=== Agenda Statistics ===');
  testAgendas.forEach((agenda, index) => {
    console.log(`${agenda.title}: ${agenda.votes} votes`);
  });
  
  // Report traffic distribution
  const totalHotVotes = hotAgendas.reduce((sum, agenda) => sum + agenda.votes, 0);
  const totalMediumVotes = mediumAgendas.reduce((sum, agenda) => sum + agenda.votes, 0);
  const totalVotes = totalHotVotes + totalMediumVotes;
  
  console.log('\n=== Traffic Distribution ===');
  console.log(`Hot agenda votes: ${totalHotVotes} (${totalVotes > 0 ? ((totalHotVotes / totalVotes) * 100).toFixed(1) : 0}%)`);
  console.log(`Medium agenda votes: ${totalMediumVotes} (${totalVotes > 0 ? ((totalMediumVotes / totalVotes) * 100).toFixed(1) : 0}%)`);
  console.log(`Result checks: ${resultChecks.count}`);
  
  // Final results for each agenda
  console.log('\n=== Final Results ===');
  testAgendas.forEach((agenda) => {
    const results = getResults(http, baseUrl, agenda.id);
    if (results.success) {
      console.log(`${agenda.title}: ${results.results.yesVotes} Yes, ${results.results.noVotes} No (${results.results.status})`);
    }
  });
}
