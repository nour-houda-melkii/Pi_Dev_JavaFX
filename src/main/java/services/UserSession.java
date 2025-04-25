package services;

import models.User;

/**
 * Classe Singleton qui gère la session de l'utilisateur connecté
 */
public class UserSession {
    private static UserSession instance;
    private User loggedInUser;

    private UserSession() {
        // Constructeur privé pour le pattern Singleton
    }

    /**
     * Récupère l'instance unique de UserSession
     * @return l'instance de UserSession
     */
    public static UserSession getInstance() {
        if (instance == null) {
            instance = new UserSession();
        }
        return instance;
    }

    /**
     * Définit l'utilisateur connecté
     * @param user l'utilisateur connecté
     */
    public void setLoggedInUser(User user) {
        this.loggedInUser = user;
    }

    /**
     * Récupère l'utilisateur connecté
     * @return l'utilisateur connecté
     */
    public User getLoggedInUser() {
        return loggedInUser;
    }

    /**
     * Déconnecte l'utilisateur actuel
     */
    public void logout() {
        this.loggedInUser = null;
    }

    /**
     * Vérifie si un utilisateur est connecté
     * @return true si un utilisateur est connecté, false sinon
     */
    public boolean isLoggedIn() {
        return loggedInUser != null;
    }
} 