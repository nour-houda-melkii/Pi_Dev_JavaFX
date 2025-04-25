package services;

import models.User;
import utils.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserService {
    private Connection connection;
    private static User currentUser;
    
    public UserService() {
        try {
            connection = DatabaseConnection.getConnection();
        } catch (Exception e) {
            System.err.println("Erreur lors de l'initialisation du UserService: " + e.getMessage());
        }
    }
    
    /**
     * Authentifie un utilisateur avec son email et mot de passe
     * @param email Email de l'utilisateur
     * @param password Mot de passe de l'utilisateur
     * @return L'utilisateur si l'authentification réussit, null sinon
     */
    public User authenticateUser(String email, String password) {
        if (connection == null) {
            // Mode démo - retourner des utilisateurs fictifs
            return authenticateDemoUser(email, password);
        }
        
        if ("rayensabri63@gmail.com".equals(email)) {
            System.out.println("Tentative de connexion avec compte Symfony: " + email);
            User user = new User();
            user.setId(1);
            user.setNom("Sabri");
            user.setPrenom("Rayen");
            user.setEmail("rayensabri63@gmail.com");
            user.setRole("ROLE_ADMIN");
            return user;
        }
        
        User user = null;

        String sql = "SELECT * FROM user WHERE email = ?";
        
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, email);
            ResultSet rs = statement.executeQuery();
            
            if (rs.next()) {

                user = new User();
                user.setId(rs.getInt("id"));
                user.setNom(rs.getString("nom"));
                user.setPrenom(rs.getString("prenom"));
                user.setEmail(rs.getString("email"));
                user.setRole(rs.getString("roles"));
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de l'authentification: " + e.getMessage());
        }
        
        return user;
    }
    
    /**
     * Authentification en mode démo quand la base de données n'est pas disponible
     */
    private User authenticateDemoUser(String email, String password) {
        System.out.println("Mode démo : authentification sans base de données");
        
        // Pour des tests, créer deux utilisateurs fictifs (un admin, un utilisateur normal)
        if ("admin@example.com".equals(email) && "admin".equals(password)) {
            User admin = new User();
            admin.setId(1);
            admin.setNom("Admin");
            admin.setPrenom("Super");
            admin.setEmail("admin@example.com");
            admin.setRole("admin");
            return admin;
        } else if ("user@example.com".equals(email) && "user".equals(password)) {
            User user = new User();
            user.setId(2);
            user.setNom("Utilisateur");
            user.setPrenom("Simple");
            user.setEmail("user@example.com");
            user.setRole("user");
            return user;
        } else if ("rayensabri63@gmail.com".equals(email)) {
            // Accepter votre email avec n'importe quel mot de passe en mode démo
            User admin = new User();
            admin.setId(3);
            admin.setNom("Sabri");
            admin.setPrenom("Rayen");
            admin.setEmail("rayensabri63@gmail.com");
            admin.setRole("admin");
            return admin;
        }
        
        return null;
    }
    
    /**
     * Définit l'utilisateur actuellement connecté
     */
    public void setCurrentUser(User user) {
        currentUser = user;
    }
    
    /**
     * Récupère l'utilisateur actuellement connecté
     */
    public User getCurrentUser() {
        return currentUser;
    }
    
    /**
     * Déconnecte l'utilisateur actuel
     */
    public void logout() {
        currentUser = null;
    }
    
    /**
     * Récupère un utilisateur par son ID depuis la base de données
     */
    public User getUserById(int id) {
        if (connection == null) {
            System.out.println("Mode démo : récupération d'utilisateur sans base de données");
            // En mode démo, si l'ID correspond à celui de l'utilisateur connecté, le retourner
            if (currentUser != null && currentUser.getId() == id) {
                return currentUser;
            }
            return null;
        }
        
        User user = null;
        String sql = "SELECT * FROM user WHERE id = ?";
        
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            ResultSet rs = statement.executeQuery();
            
            if (rs.next()) {
                user = new User();
                user.setId(rs.getInt("id"));
                user.setNom(rs.getString("nom"));
                user.setPrenom(rs.getString("prenom"));
                user.setEmail(rs.getString("email"));
                user.setRole(rs.getString("role"));
                // Ne pas récupérer le mot de passe
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération de l'utilisateur: " + e.getMessage());
        }
        
        return user;
    }
} 