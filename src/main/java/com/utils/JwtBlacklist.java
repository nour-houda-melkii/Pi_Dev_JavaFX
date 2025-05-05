package com.utils;

import java.util.HashSet;
import java.util.Set;

public class JwtBlacklist {
    private static final Set<String> blacklistedTokens = new HashSet<>();

    // Ajoute un token à la liste noire
    public static void invalidateToken(String token) {
        blacklistedTokens.add(token);
    }

    // Vérifie si un token est invalide
    public static boolean isTokenBlacklisted(String token) {
        return blacklistedTokens.contains(token);
    }

    // Vide la liste (pour les tests)
    public static void clear() {
        blacklistedTokens.clear();
    }
}