// LoginResponse.java
package com.dto;

import com.models.User;

public class LoginResponse {
    private boolean success;
    private String message;
    private User user;
    private String token;

    // Pour les échecs
    public LoginResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    // Pour les succès
    public LoginResponse(boolean success, String message, User user, String token) {
        this.success = success;
        this.message = message;
        this.user = user;
        this.token = token;
    }

    // Getters
    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public User getUser() { return user; }
    public String getToken() { return token; }
}