package com.utils;


import java.util.prefs.Preferences;

public class AuthManager {
    private static final String TOKEN_KEY = "jwt_token";
    private static Preferences prefs = Preferences.userRoot().node(AuthManager.class.getName());

    public static void storeToken(String token) {
        prefs.put(TOKEN_KEY, token);
    }

    public static String getStoredToken() {
        return prefs.get(TOKEN_KEY, null);
    }

    public static void clearToken() {
        prefs.remove(TOKEN_KEY);
    }
}
