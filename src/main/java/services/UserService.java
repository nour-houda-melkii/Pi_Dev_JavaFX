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
        // Utiliser hardcoded user pour rayensabri63@gmail.com
        if ("rayensabri63@gmail.com".equals(email)) {
            System.out.println("Connexion avec le compte utilisateur prédéfini: " + email);
            User user = new User();
            user.setId(1);
            user.setEmail("rayensabri63@gmail.com");
            user.setRole("ROLE_ADMIN");
            
            // Si possible, définir le nom et prénom (mais pas obligatoire)
            try {
                user.setNom("Sabri");
                user.setPrenom("Rayen");
            } catch (Exception e) {
                System.out.println("Note: Les champs nom/prénom ne sont pas disponibles mais ce n'est pas bloquant");
            }
            
            // Mettre à jour le currentUser
            this.currentUser = user;
            return user;
        }
        
        // Pour les autres utilisateurs, essayer la base de données
        // Si la connexion est null, utiliser le mode démo
        if (connection == null) {
            return authenticateDemoUser(email, password);
        }
        
        User user = null;
        String sql = "SELECT * FROM user WHERE email = ?";
        
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, email);
            ResultSet rs = statement.executeQuery();
            
            if (rs.next()) {
                user = new User();
                user.setId(rs.getInt("id"));
                user.setEmail(rs.getString("email"));
                
                // Essayer de lire les colonnes optionnelles de manière sécurisée
                try {
                    user.setNom(rs.getString("nom"));
                } catch (SQLException e) {
                    System.out.println("Colonne 'nom' non disponible dans la base de données");
                }
                
                try {
                    user.setPrenom(rs.getString("prenom"));
                } catch (SQLException e) {
                    System.out.println("Colonne 'prenom' non disponible dans la base de données");
                }
                
                try {
                    user.setRole(rs.getString("roles"));
                } catch (SQLException e) {
                    try {
                        user.setRole(rs.getString("role"));
                    } catch (SQLException e2) {
                        System.out.println("Colonnes 'roles' et 'role' non disponibles, utilisation de la valeur par défaut");
                        user.setRole("ROLE_USER");
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de l'authentification: " + e.getMessage());
            return null; // Retourner null en cas d'erreur
        }
        
        // Mettre à jour le currentUser
        this.currentUser = user;
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
            try {
                admin.setNom("Admin");
                admin.setPrenom("Super");
            } catch (Exception e) {
                // Ignorer si les champs ne sont pas disponibles
            }
            admin.setEmail("admin@example.com");
            admin.setRole("ROLE_ADMIN");
            return admin;
        } else if ("user@example.com".equals(email) && "user".equals(password)) {
            User user = new User();
            user.setId(2);
            try {
                user.setNom("Utilisateur");
                user.setPrenom("Simple");
            } catch (Exception e) {
                // Ignorer si les champs ne sont pas disponibles
            }
            user.setEmail("user@example.com");
            user.setRole("ROLE_USER");
            return user;
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
                user.setEmail(rs.getString("email"));
                
                // Essayer de lire les colonnes optionnelles de manière sécurisée
                try {
                    user.setNom(rs.getString("nom"));
                } catch (SQLException e) {
                    // Ignorer si la colonne n'existe pas
                }
                
                try {
                    user.setPrenom(rs.getString("prenom"));
                } catch (SQLException e) {
                    // Ignorer si la colonne n'existe pas
                }
                
                try {
                    user.setRole(rs.getString("roles"));
                } catch (SQLException e) {
                    try {
                        user.setRole(rs.getString("role"));
                    } catch (SQLException e2) {
                        user.setRole("ROLE_USER");
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération de l'utilisateur: " + e.getMessage());
        }
        
        return user;
    }
} 