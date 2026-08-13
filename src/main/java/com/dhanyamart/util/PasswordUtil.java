package com.dhanyamart.util;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Password security helper.
 *
 * Plain-text passwords are NEVER stored. Each password is hashed with
 * PBKDF2WithHmacSHA256 + a random 16-byte salt (10000 iterations).
 *
 * Stored format in Excel:  salt (Base64) + ":" + hash (Base64)
 *
 * Example stored value:
 *   oKx7fH2nP9...:Qk4EwRtY...      <- these are NOT the password, cannot be reversed
 */
public class PasswordUtil {

    private static final int ITERATIONS = 10000;
    private static final int KEY_LENGTH_BITS = 256;
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";

    /**
     * Creates a salted hash of the given plain-text password.
     */
    public static String hashPassword(String plainPassword) {
        try {
            byte[] salt = new byte[16];
            new SecureRandom().nextBytes(salt);

            PBEKeySpec spec = new PBEKeySpec(plainPassword.toCharArray(), salt, ITERATIONS, KEY_LENGTH_BITS);
            SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM);
            byte[] hash = factory.generateSecret(spec).getEncoded();

            String saltText = Base64.getEncoder().encodeToString(salt);
            String hashText = Base64.getEncoder().encodeToString(hash);

            return saltText + ":" + hashText;
        } catch (Exception e) {
            throw new RuntimeException("Password hashing failed", e);
        }
    }

    /**
     * Verifies a plain-text password against a stored "salt:hash" value.
     * Returns true only if the password matches.
     */
    public static boolean verifyPassword(String plainPassword, String storedValue) {
        if (plainPassword == null || storedValue == null) {
            return false;
        }
        try {
            String[] parts = storedValue.split(":");
            if (parts.length != 2) {
                return false;
            }

            byte[] salt = Base64.getDecoder().decode(parts[0]);
            byte[] expectedHash = Base64.getDecoder().decode(parts[1]);

            PBEKeySpec spec = new PBEKeySpec(plainPassword.toCharArray(), salt, ITERATIONS, KEY_LENGTH_BITS);
            SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM);
            byte[] actualHash = factory.generateSecret(spec).getEncoded();

            // isEqual = constant-time comparison (no timing leaks)
            return MessageDigest.isEqual(expectedHash, actualHash);
        } catch (Exception e) {
            return false;
        }
    }
}
