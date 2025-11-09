package com.duncodi.ppslink.stanchart.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

public class PasswordEncryptionUtil {

    private static final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    // Private constructor to prevent instantiation
    private PasswordEncryptionUtil() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Encrypts a raw password using BCrypt hashing algorithm
     * @param rawPassword The plain text password to encrypt
     * @return The hashed password
     */
    public static String encryptPassword(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    /**
     * Verifies if a raw password matches the encrypted password
     * @param rawPassword The plain text password to verify
     * @param encryptedPassword The stored hashed password
     * @return true if passwords match, false otherwise
     */
    public static boolean matches(String rawPassword, String encryptedPassword) {
        return passwordEncoder.matches(rawPassword, encryptedPassword);
    }

    /**
     * Generates a random salt (included automatically in BCrypt)
     * @return A salt string (part of BCrypt's output)
     */
    public static String generateSalt() {
        return passwordEncoder.encode("");
    }
}