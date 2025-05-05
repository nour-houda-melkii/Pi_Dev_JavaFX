package com.utils;

import org.mindrot.jbcrypt.BCrypt;

public class PasswordHasher {
    private static final int LOG_ROUNDS = 13; // Doit correspondre à la config Symfony

    // Garde le nom original mais produit un hash compatible Symfony
    public static String hashPassword(String plainPassword) {
        if (plainPassword == null || plainPassword.isEmpty()) {
            throw new IllegalArgumentException("Password cannot be empty");
        }

        try {
            // Génère un hash BCrypt standard ($2a$)
            String hash = BCrypt.hashpw(plainPassword, BCrypt.gensalt(LOG_ROUNDS));
            // Convertit en format Symfony ($2y$)
            return convertToSymfonyHash(hash);
        } catch (Exception e) {
            throw new RuntimeException("Password hashing failed", e);
        }
    }

    // Garde le nom original mais vérifie les hashs Symfony
    public static boolean checkPassword(String plainPassword, String hashedPassword) {
        if (plainPassword == null || hashedPassword == null) {
            return false;
        }

        try {
            // Convertit le hash pour BCrypt si c'est un hash Symfony
            String normalizedHash = normalizeHash(hashedPassword);
            return BCrypt.checkpw(plainPassword, normalizedHash);
        } catch (Exception e) {
            System.err.println("Password check failed: " + e.getMessage());
            return false;
        }
    }

    private static String convertToSymfonyHash(String bcryptHash) {
        if (bcryptHash.startsWith("$2a$")) {
            return "$2y$" + bcryptHash.substring(4);
        }
        return bcryptHash;
    }

    private static String normalizeHash(String hash) {
        if (hash.startsWith("$2y$")) {
            return "$2a$" + hash.substring(4);
        }
        return hash;
    }
}