package controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import models.User;
import services.UserService;
import utils.SessionManager;

import java.io.IOException;

public class LoginController {
    @FXML
    private TextField emailField;
    
    @FXML
    private PasswordField passwordField;
    
    @FXML
    private Label errorMessage;
    
    private UserService userService;
    
    @FXML
    public void initialize() {
        userService = new UserService();
    }
    
    @FXML
    private void handleLogin() {
        String email = emailField.getText().trim();
        String password = passwordField.getText();
        
        // Validation simple des champs
        if (email.isEmpty() || password.isEmpty()) {
            showError("Veuillez remplir tous les champs");
            return;
        }
        
        // Tenter de se connecter
        try {
            User user = userService.authenticateUser(email, password);
            
            if (user != null) {
                // Stocker l'utilisateur connecté à la fois dans UserService et SessionManager
                userService.setCurrentUser(user);
                SessionManager.setCurrentUser(user);
                
                // Afficher les informations sur l'utilisateur connecté
                System.out.println("Utilisateur connecté: " + user.getEmail() + ", Rôle: " + user.getRole());
                
                // Déterminer quelle interface montrer selon le rôle
                String role = user.getRole();
                if (role != null) {
                    role = role.toUpperCase();
                    if (role.contains("USER") || role.equals("USER")) {
                        System.out.println("Ouverture de l'interface frontend (utilisateur standard)");
                        openFrontInterface();
                    } else {
                        System.out.println("Ouverture de l'interface admin (role: " + role + ")");
                        openAdminInterface();
                    }
                } else {
                    // Par défaut ou pour le rôle "admin", ouvrir l'interface d'administration
                    System.out.println("Rôle non défini, ouverture de l'interface admin par défaut");
                    openAdminInterface();
                }
            } else {
                showError("Email ou mot de passe incorrect");
            }
        } catch (Exception e) {
            showError("Erreur de connexion: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void showError(String message) {
        errorMessage.setText(message);
        errorMessage.setVisible(true);
    }
    
    private void openFrontInterface() {
        try {
            // Fermer la fenêtre de login
            Stage currentStage = (Stage) emailField.getScene().getWindow();
            
            // Ouvrir l'interface frontend complète avec navbar
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/front_main.fxml"));
            Parent root = loader.load();
            
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/styles/style.css").toExternalForm());
            
            Stage frontStage = new Stage();
            frontStage.setTitle("SAHATECH - Événements");
            frontStage.setScene(scene);
            frontStage.setMaximized(true);
            
            // Obtenir le contrôleur principal et lui passer l'utilisateur
            controllers.FrontMainController controller = loader.getController();
            
            // Tenter de passer l'utilisateur si le contrôleur a une méthode setUser
            try {
                java.lang.reflect.Method setUserMethod = controller.getClass().getMethod("setUser", models.User.class);
                setUserMethod.invoke(controller, userService.getCurrentUser());
                System.out.println("Utilisateur transmis au contrôleur principal frontend avec succès");
            } catch (Exception e) {
                System.out.println("Le contrôleur FrontMainController n'a pas de méthode setUser, mais l'utilisateur est disponible via SessionManager");
            }
            
            // Fermer l'ancienne fenêtre et afficher la nouvelle
            currentStage.close();
            frontStage.show();
            
        } catch (IOException e) {
            showError("Erreur lors de l'ouverture de l'interface: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void openAdminInterface() {
        try {
            // Fermer la fenêtre de login
            Stage currentStage = (Stage) emailField.getScene().getWindow();
            
            // Ouvrir l'interface d'administration avec le sidebar
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main.fxml"));
            Parent root = loader.load();
            
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/styles/style.css").toExternalForm());
            
            Stage adminStage = new Stage();
            adminStage.setTitle("Administration");
            adminStage.setScene(scene);
            adminStage.setMaximized(true);
            
            // Obtenir le contrôleur et lui passer l'utilisateur si nécessaire
            org.example.controllers.MainController controller = loader.getController();
            
            // Si la méthode setUser existe dans MainController, on peut l'utiliser
            try {
                // Essayons d'accéder à la méthode setUser par réflexion
                java.lang.reflect.Method setUserMethod = controller.getClass().getMethod("setUser", models.User.class);
                setUserMethod.invoke(controller, userService.getCurrentUser());
                System.out.println("Utilisateur transmis au contrôleur principal avec succès");
            } catch (Exception e) {
                System.out.println("La méthode setUser n'existe pas dans MainController - C'est normal si elle n'a pas été implémentée");
                // Ce n'est pas grave, on continue sans passer l'utilisateur
            }
            
            // Fermer l'ancienne fenêtre et afficher la nouvelle
            currentStage.close();
            adminStage.show();
            
        } catch (IOException e) {
            showError("Erreur lors de l'ouverture de l'interface: " + e.getMessage());
            e.printStackTrace();
        }
    }
} 