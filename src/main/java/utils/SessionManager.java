package utils;

import models.User;

/**
 * Classe utilitaire pour gérer la session utilisateur dans toute l'application
 */
public class SessionManager {
    private static User currentUser;
    
    /**
     * Définit l'utilisateur actuellement connecté
     */
    public static void setCurrentUser(User user) {
        currentUser = user;
        System.out.println("SessionManager: Utilisateur défini - " + 
            (user != null ? user.getEmail() + " (rôle: " + user.getRole() + ")" : "null"));
    }
    
    /**
     * Récupère l'utilisateur actuellement connecté
     */
    public static User getCurrentUser() {
        return currentUser;
    }
    
    /**
     * Vérifie si un utilisateur est connecté
     */
    public static boolean isLoggedIn() {
        return currentUser != null;
    }
    
    /**
     * Vérifie si l'utilisateur connecté est un administrateur
     */
    public static boolean isAdmin() {
        if (currentUser == null || currentUser.getRole() == null) {
            return false;
        }
        
        String role = currentUser.getRole().toUpperCase();
        return role.contains("ADMIN") || role.equals("ADMIN");
    }
    
    /**
     * Déconnecte l'utilisateur
     */
    public static void logout() {
        currentUser = null;
        System.out.println("SessionManager: Utilisateur déconnecté");
    }
} 