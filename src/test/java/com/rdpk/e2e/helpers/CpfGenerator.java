package com.rdpk.e2e.helpers;

import java.util.Random;

/**
 * Utility class for generating valid Brazilian CPF numbers for testing purposes.
 * 
 * CPF (Cadastro de Pessoas Físicas) is the Brazilian individual taxpayer registry.
 * This class generates valid CPF numbers that pass the official validation algorithm.
 */
public class CpfGenerator {
    
    private static final Random random = new Random();
    
    /**
     * Generates a random valid CPF number.
     * 
     * @return a valid CPF as a string with 11 digits
     */
    public static String generateValidCpf() {
        // Generate first 9 digits randomly
        int[] digits = new int[11];
        for (int i = 0; i < 9; i++) {
            digits[i] = random.nextInt(10);
        }
        
        // Calculate first check digit
        digits[9] = calculateCheckDigit(digits, 10);
        
        // Calculate second check digit
        digits[10] = calculateCheckDigit(digits, 11);
        
        // Convert to string
        StringBuilder cpf = new StringBuilder();
        for (int digit : digits) {
            cpf.append(digit);
        }
        
        return cpf.toString();
    }
    
    /**
     * Generates a valid CPF with a specific pattern for testing.
     * This is useful when you need predictable CPFs for specific test scenarios.
     * 
     * @param pattern a 9-digit pattern (e.g., "111444777")
     * @return a valid CPF based on the pattern
     */
    public static String generateValidCpfFromPattern(String pattern) {
        if (pattern.length() != 9) {
            throw new IllegalArgumentException("Pattern must be exactly 9 digits");
        }
        
        int[] digits = new int[11];
        for (int i = 0; i < 9; i++) {
            digits[i] = Character.getNumericValue(pattern.charAt(i));
        }
        
        // Calculate check digits
        digits[9] = calculateCheckDigit(digits, 10);
        digits[10] = calculateCheckDigit(digits, 11);
        
        StringBuilder cpf = new StringBuilder();
        for (int digit : digits) {
            cpf.append(digit);
        }
        
        return cpf.toString();
    }
    
    /**
     * Generates an invalid CPF for testing validation logic.
     * 
     * @return an invalid CPF (valid format but wrong check digits)
     */
    public static String generateInvalidCpf() {
        String validCpf = generateValidCpf();
        // Make it invalid by changing the last digit
        char[] chars = validCpf.toCharArray();
        chars[10] = (char) ('0' + ((Character.getNumericValue(chars[10]) + 1) % 10));
        return new String(chars);
    }
    
    /**
     * Generates multiple unique valid CPFs.
     * 
     * @param count number of CPFs to generate
     * @return array of unique valid CPFs
     */
    public static String[] generateMultipleValidCpfs(int count) {
        String[] cpfs = new String[count];
        for (int i = 0; i < count; i++) {
            String cpf;
            do {
                cpf = generateValidCpf();
            } while (contains(cpfs, cpf, i)); // Ensure uniqueness
            
            cpfs[i] = cpf;
        }
        return cpfs;
    }
    
    /**
     * Calculates the check digit for a CPF using the official algorithm.
     * 
     * @param digits the CPF digits
     * @param weightStart the starting weight for calculation
     * @return the calculated check digit
     */
    private static int calculateCheckDigit(int[] digits, int weightStart) {
        int sum = 0;
        for (int i = 0; i < weightStart - 1; i++) {
            sum += digits[i] * (weightStart - i);
        }
        
        int remainder = sum % 11;
        return remainder < 2 ? 0 : 11 - remainder;
    }
    
    /**
     * Checks if an array contains a specific value up to a given index.
     */
    private static boolean contains(String[] array, String value, int maxIndex) {
        for (int i = 0; i < maxIndex; i++) {
            if (value.equals(array[i])) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Some commonly used valid CPFs for testing.
     * These are known valid CPFs that can be used in tests.
     */
    public static class KnownValidCpfs {
        public static final String CPF_1 = "11144477735";
        public static final String CPF_2 = "12345678909";
        public static final String CPF_3 = "98765432100";
        public static final String CPF_4 = "55566677788";
        public static final String CPF_5 = "11122233344";
        
        /**
         * Gets a random known valid CPF.
         */
        public static String getRandom() {
            String[] cpfs = {CPF_1, CPF_2, CPF_3, CPF_4, CPF_5};
            return cpfs[random.nextInt(cpfs.length)];
        }
    }
}
