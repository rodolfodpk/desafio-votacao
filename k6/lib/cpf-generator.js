/**
 * CPF Generator for K6 Performance Tests
 * Generates valid and invalid CPFs for testing
 */

/**
 * Generate a valid CPF using the official algorithm
 */
export function generateValidCpf() {
  // Generate 9 random digits
  const digits = [];
  for (let i = 0; i < 9; i++) {
    digits.push(Math.floor(Math.random() * 10));
  }

  // Calculate first check digit
  let sum = 0;
  for (let i = 0; i < 9; i++) {
    sum += digits[i] * (10 - i);
  }
  let firstCheck = 11 - (sum % 11);
  if (firstCheck >= 10) firstCheck = 0;
  digits.push(firstCheck);

  // Calculate second check digit
  sum = 0;
  for (let i = 0; i < 10; i++) {
    sum += digits[i] * (11 - i);
  }
  let secondCheck = 11 - (sum % 11);
  if (secondCheck >= 10) secondCheck = 0;
  digits.push(secondCheck);

  return digits.join('');
}

/**
 * Generate an invalid CPF (fails validation)
 */
export function generateInvalidCpf() {
  // Generate 11 random digits without validation
  const digits = [];
  for (let i = 0; i < 11; i++) {
    digits.push(Math.floor(Math.random() * 10));
  }
  
  // Ensure it's actually invalid by making sure check digits are wrong
  const cpf = digits.join('');
  if (isValidCpf(cpf)) {
    // If by chance it's valid, make it invalid by changing last digit
    return cpf.substring(0, 10) + ((parseInt(cpf[10]) + 1) % 10);
  }
  
  return cpf;
}

/**
 * Validate a CPF using the official algorithm
 */
export function isValidCpf(cpf) {
  // Remove non-digits
  cpf = cpf.replace(/\D/g, '');
  
  // Check length
  if (cpf.length !== 11) return false;
  
  // Check if all digits are the same (invalid)
  if (/^(\d)\1{10}$/.test(cpf)) return false;
  
  // Calculate first check digit
  let sum = 0;
  for (let i = 0; i < 9; i++) {
    sum += parseInt(cpf[i]) * (10 - i);
  }
  let firstCheck = 11 - (sum % 11);
  if (firstCheck >= 10) firstCheck = 0;
  
  // Check first digit
  if (parseInt(cpf[9]) !== firstCheck) return false;
  
  // Calculate second check digit
  sum = 0;
  for (let i = 0; i < 10; i++) {
    sum += parseInt(cpf[i]) * (11 - i);
  }
  let secondCheck = 11 - (sum % 11);
  if (secondCheck >= 10) secondCheck = 0;
  
  // Check second digit
  return parseInt(cpf[10]) === secondCheck;
}

/**
 * Generate a list of unique valid CPFs
 */
export function generateUniqueValidCpfs(count) {
  const cpfs = new Set();
  
  while (cpfs.size < count) {
    cpfs.add(generateValidCpf());
  }
  
  return Array.from(cpfs);
}

/**
 * Generate a list of invalid CPFs
 */
export function generateInvalidCpfs(count) {
  const cpfs = [];
  
  for (let i = 0; i < count; i++) {
    cpfs.push(generateInvalidCpf());
  }
  
  return cpfs;
}

/**
 * Generate CPFs for testing different scenarios
 */
export function generateTestCpfs(validCount = 1000, invalidCount = 100) {
  return {
    valid: generateUniqueValidCpfs(validCount),
    invalid: generateInvalidCpfs(invalidCount),
    // Some known valid CPFs for consistent testing
    knownValid: [
      '11144477735', // Known valid CPF
      '12345678909', // Known valid CPF
      '98765432100', // Known valid CPF
    ],
    // Some known invalid CPFs
    knownInvalid: [
      '00000000000', // All zeros
      '11111111111', // All ones
      '12345678901', // Invalid check digits
    ]
  };
}

/**
 * Get a CPF from a predefined list based on VU and iteration
 */
export function getCpfForVu(cpfList, vuId, iteration, type = 'valid') {
  const cpfs = cpfList[type] || cpfList.valid;
  const index = (vuId * 1000 + iteration) % cpfs.length;
  return cpfs[index];
}

/**
 * Format CPF with dots and dash (XXX.XXX.XXX-XX)
 */
export function formatCpf(cpf) {
  cpf = cpf.replace(/\D/g, '');
  return cpf.replace(/(\d{3})(\d{3})(\d{3})(\d{2})/, '$1.$2.$3-$4');
}

/**
 * Remove formatting from CPF
 */
export function unformatCpf(cpf) {
  return cpf.replace(/\D/g, '');
}
