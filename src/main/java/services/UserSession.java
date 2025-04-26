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
        System.out.println("UserSession: utilisateur défini - " + 
            (user != null ? user.getEmail() + " (rôle: " + user.getRole() + ")" : "null"));
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
        return loggedInUser != null && loggedInUser.getEmail() != null && !loggedInUser.getEmail().isEmpty();
    }
    
    /**
     * Crée un utilisateur invité temporaire
     * Utilisé pour éviter les erreurs quand aucun utilisateur n'est connecté
     */
    public User createGuestUser() {
        User guest = new User();
        guest.setId(-1);  // ID négatif pour indiquer qu'il s'agit d'un invité
        guest.setEmail("guest@temp.com");
        guest.setRole("GUEST");
        return guest;
    }
} 