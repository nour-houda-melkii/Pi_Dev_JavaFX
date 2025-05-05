package com.utils;

import java.util.prefs.Preferences;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AuthManager {
    private static final Logger LOGGER = Logger.getLogger(AuthManager.class.getName());
    private static final String TOKEN_KEY = "jwt_token";
    private static final String USER_ID_KEY = "user_id";
    private static Preferences prefs = Preferences.userRoot().node(AuthManager.class.getName());

    // Static variable to hold the user ID in memory during the application session
    private static int cachedUserId = -1;

    // Static variable to hold the token in memory during the application session
    private static String cachedToken = null;

    public static void storeToken(String token) {
        if (token == null || token.isEmpty()) {
            LOGGER.warning("Attempted to store null or empty token");
            return;
        }
        // Store in both memory and preferences
        cachedToken = token;
        prefs.put(TOKEN_KEY, token);
        LOGGER.info("Token stored successfully");
    }

    public static String getStoredToken() {
        // First check memory cache
        if (cachedToken != null && !cachedToken.isEmpty()) {
            return cachedToken;
        }

        // If not in memory, try preferences
        String token = prefs.get(TOKEN_KEY, null);
        if (token != null && !token.isEmpty()) {
            cachedToken = token; // Update memory cache
        }

        LOGGER.info("Retrieved token: " + (token != null ? "Valid token" : "No token found"));
        return token;
    }

    public static void clearToken() {
        cachedToken = null;
        prefs.remove(TOKEN_KEY);
        LOGGER.info("Token cleared");
    }

    public static void storeUserId(int id) {
        if (id <= 0) {
            LOGGER.warning("Attempted to store invalid user ID: " + id);
            return;
        }

        // Store in both memory and preferences
        cachedUserId = id;
        prefs.putInt(USER_ID_KEY, id);
        LOGGER.info("User ID stored successfully: " + id + " (Memory: " + cachedUserId + ", Prefs: " + prefs.getInt(USER_ID_KEY, -1) + ")");
    }

    public static int getUserId() {
        // First check memory cache
        if (cachedUserId > 0) {
            LOGGER.fine("Returning cached user ID: " + cachedUserId);
            return cachedUserId;
        }

        // If not in memory, try preferences
        int id = prefs.getInt(USER_ID_KEY, -1);

        if (id > 0) {
            cachedUserId = id; // Update memory cache
            LOGGER.info("Retrieved user ID from preferences: " + id);
            return id;
        }

        LOGGER.warning("No valid user ID found in memory or preferences");
        return -1;
    }

    public static void clearUserId() {
        cachedUserId = -1;
        prefs.remove(USER_ID_KEY);
        LOGGER.info("User ID cleared");
    }

    public static void clearAll() {
        clearToken();
        clearUserId();
        LOGGER.info("All auth data cleared");
    }

    public static boolean isLoggedIn() {
        boolean hasUserId = getUserId() > 0;
        boolean hasToken = getStoredToken() != null;
        boolean result = hasUserId && hasToken;

        LOGGER.info("isLoggedIn check - HasUserId: " + hasUserId +
                ", HasToken: " + hasToken +
                ", Result: " + result);

        return result;
    }

    // Debug method to verify what's in the preferences
    public static void logAuthState() {
        LOGGER.info("AUTH STATE - Memory: userId=" + cachedUserId + ", token=" + (cachedToken != null ? "present" : "null") +
                " | Preferences: userId=" + prefs.getInt(USER_ID_KEY, -1) +
                ", token=" + (prefs.get(TOKEN_KEY, null) != null ? "present" : "null"));
    }
}