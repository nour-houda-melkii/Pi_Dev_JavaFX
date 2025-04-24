package com.controllers;

import com.demo.enums.Role;
import com.exceptions.AuthException;
import com.services.AuthService;
import com.dto.LoginResponse;
import com.models.User;

public class AuthController {
    private final AuthService authService;

    public AuthController() {
        this.authService = new AuthService();
    }

    public LoginResponse handleLogin(String email, String password) throws AuthException {
        try {
            // Authentification qui retourne un JWT
            String jwtToken = authService.login(email, password);

            // Récupère l'User à partir du token
            User user = authService.getUserFromToken(jwtToken);

            return new LoginResponse(
                    true,
                    "Authentification réussie",
                    user,
                    jwtToken  // Important pour le client
            );
        } catch (AuthException e) {
            return new LoginResponse(false, e.getMessage());
        }
    }

    public boolean checkUserRole(User user, String requiredRole) {
        return authService.checkRole(user, requiredRole);
    }

    public User validateToken(String jwtToken) throws AuthException {
        return authService.getUserFromToken(jwtToken);
    }
}