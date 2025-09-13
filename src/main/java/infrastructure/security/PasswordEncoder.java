package main.java.infrastructure.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Simple password encoder using SHA-256 with salt.
 * This is a basic implementation that doesn't require external dependencies.
 * For production use, consider using BCrypt or other stronger algorithms.
 */
public final class PasswordEncoder {

    private static final String ALGORITHM = "SHA-256";
    private static final int SALT_LENGTH = 16;
    private static final String SEPARATOR = "$";

    /**
     * Hash a raw password with a random salt
     * Format: salt$hashedPassword
     */
    public String hash(String raw) {
        try {
            // Generate random salt
            byte[] salt = generateSalt();

            // Hash password with salt
            MessageDigest md = MessageDigest.getInstance(ALGORITHM);
            md.update(salt);
            byte[] hashedPassword = md.digest(raw.getBytes(StandardCharsets.UTF_8));

            // Encode salt and hash to Base64
            String saltString = Base64.getEncoder().encodeToString(salt);
            String hashString = Base64.getEncoder().encodeToString(hashedPassword);

            return saltString + SEPARATOR + hashString;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Check if a raw password matches the hashed password
     */
    public boolean matches(String raw, String hash) {
        try {
            // Split salt and hash
            String[] parts = hash.split("\\" + SEPARATOR);
            if (parts.length != 2) {
                return false;
            }

            String saltString = parts[0];
            String expectedHash = parts[1];

            // Decode salt
            byte[] salt = Base64.getDecoder().decode(saltString);

            // Hash the raw password with the same salt
            MessageDigest md = MessageDigest.getInstance(ALGORITHM);
            md.update(salt);
            byte[] hashedPassword = md.digest(raw.getBytes(StandardCharsets.UTF_8));

            // Compare hashes
            String actualHash = Base64.getEncoder().encodeToString(hashedPassword);
            return expectedHash.equals(actualHash);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Generate a random salt
     */
    private byte[] generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[SALT_LENGTH];
        random.nextBytes(salt);
        return salt;
    }
}