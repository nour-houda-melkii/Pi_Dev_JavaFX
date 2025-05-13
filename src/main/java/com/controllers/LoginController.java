package com.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.models.User;
import com.services.AuthService;
import com.utils.AlertUtils;
import com.utils.AuthManager;
import com.utils.JwtUtil;
import com.utils.SimpleCaptcha;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.io.IOException;

public class LoginController {
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private CheckBox rememberMeCheckbox;
    @FXML private Label errorLabel;

    private SimpleCaptcha simpleCaptcha;
    @FXML private StackPane captchaContainer;


    private AuthService authService;

    @FXML
    public void initialize() {
        this.authService = new AuthService(); // Initialisation tardive
        initializeSimpleCaptcha();
    }




    public LoginController() {
        this.authService = new AuthService(); // Initialisation directe
    }

    // Ajoutez cette méthode
    public void setAuthService(AuthService authService) {
        this.authService = authService;
    }


    private void initializeSimpleCaptcha() {
        Platform.runLater(() -> {
            try {
                simpleCaptcha = new SimpleCaptcha();
                captchaContainer.getChildren().add(simpleCaptcha);
            } catch (Exception e) {
                e.printStackTrace();
                Label fallbackLabel = new Label("Erreur de chargement du CAPTCHA");
                fallbackLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                captchaContainer.getChildren().add(fallbackLabel);
            }
        });
    }

    @FXML
    private void handleLogin() {
        if (simpleCaptcha == null || !simpleCaptcha.validateCaptcha()) {
            showError("Veuillez compléter correctement la vérification CAPTCHA");
            return;
        }

        String email = emailField.getText().trim();
        String password = passwordField.getText().trim();

        if (email.isEmpty() || password.isEmpty()) {
            showError("Veuillez remplir tous les champs obligatoires");
            return;
        }

        try {
            // Votre code d'authentification existant
            String token = authService.login(email, password);
            AuthManager.storeToken(token);
            User user = authService.getUserFromToken(token);

            if (user.getRoles().contains("ROLE_MEDECIN") || user.getRoles().contains("ROLE_ADMIN")) {
                redirectToAdminDashboard();
            } else {
                // Conversion explicite vers com.event.models.User
                com.event.models.User eventUser = new com.event.models.User();
                eventUser.setId(user.getId());
                eventUser.setEmail(user.getEmail());
                eventUser.setPassword(user.getPassword());
                eventUser.setFirstName(user.getFirstName());
                eventUser.setLastName(user.getLastName());
                eventUser.setRole(user.getRoles().isEmpty() ? null : user.getRoles().get(0));
                eventUser.setRoles(user.getRoles());
                eventUser.setGender(user.getGender() != null ? user.getGender().name() : null);
                eventUser.setAdress(user.getAddress());
                eventUser.setPhoneNumber(user.getPhoneNumber());
                eventUser.setNumeroLicence(user.getNumeroLicence());
                eventUser.setAge(user.getAge());
                eventUser.setSpecialite(user.getSpecialite() != null ? user.getSpecialite().name() : null);
                eventUser.setMedicalFile(user.getMedicalFile());
                eventUser.setVerified(user.isVerified());
                eventUser.setStatus(user.getStatus());
                redirectToFrontOffice(eventUser);
            }

        } catch (Exception e) {
            // Gestion des erreurs améliorée
            String errorMessage = e.getMessage();

            // Messages spécifiques pour les différents cas d'erreur
            if (errorMessage.contains("désactivé pour inactivité")) {
                showErrorWithHelpLink(errorMessage, "Contactez le support");
            } else if (errorMessage.contains("temporarily locked")) {
                showErrorWithTimer(errorMessage);
            } else {
                showError(errorMessage);
            }
            // Réinitialiser le CAPTCHA après une tentative échouée
            simpleCaptcha.reset();
        }
    }

    private void showErrorWithTimer(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);

        // Compte à rebours pour le déblocage
        new Thread(() -> {
            for (int i = 30; i > 0; i--) {
                final int remaining = i;
                Platform.runLater(() -> {
                    errorLabel.setText(message + " Déblocage dans " + remaining + " minutes");
                });
                try {
                    Thread.sleep(60000); // 1 minute
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            Platform.runLater(() -> {
                errorLabel.setText("");
            });
        }).start();
    }


    // Méthode pour afficher les erreurs avec un lien d'aide
    private void showErrorWithHelpLink(String message, String linkText) {
        Hyperlink helpLink = new Hyperlink(linkText);
        helpLink.setOnAction(e -> {
            // Ouvrir une fenêtre d'aide ou composer un email
            AlertUtils.showInfoAlert("Assistance", "Veuillez contacter support@votreapp.com");
        });

        errorLabel.setGraphic(helpLink);
        errorLabel.setText(message + " ");
        errorLabel.setContentDisplay(ContentDisplay.RIGHT);
        errorLabel.setVisible(true);
    }

    private boolean verifyRecaptcha(String recaptchaResponse) {
        // Pour les applications desktop
        if ("desktop_app_bypass".equals(recaptchaResponse)) {
            System.out.println("Application desktop - bypass reCAPTCHA");
            return true;
        }

        // Si vous êtes en mode développement (pour les tests)
        if (System.getProperty("dev.mode") != null) {
            System.out.println("Mode développement - bypass reCAPTCHA");
            return true;
        }

        // Pour la production, vous pourriez implémenter une autre approche de sécurité
        // Le code d'origine reste commenté pour référence
    /*
    try {
        HttpClient client = HttpClient.newHttpClient();
        String formData = "secret=" + SECRET_KEY + "&response=" + recaptchaResponse;
        ...
    */

        // En mode desktop, nous ne pouvons pas vraiment vérifier avec l'API Google
        return true;
    }


    private void redirectToAdminDashboard() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/views/AdminDashboard.fxml"));
            Parent root = loader.load();

            // Passer le token si nécessaire
            AdminDashboardController controller = loader.getController();
            controller.setToken(AuthManager.getStoredToken());  // <-- Passage du token

            Stage stage = (Stage) emailField.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setMaximized(true);
            stage.show();
        } catch (IOException e) {
            showError("Erreur lors du chargement du dashboard");
            e.printStackTrace();
        }
    }

    // Ajoutez cette nouvelle méthode dans le même contrôleur
    private void redirectToFrontOffice(com.event.models.User user) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/views/event/front_main.fxml"));
            Parent root = loader.load();

            // Récupérer le contrôleur principal du front
            com.event.controllers.FrontMainController controller = loader.getController();

            // Propager l'utilisateur dans la session globale
            com.event.utils.SessionManager.setCurrentUser(user);

            // Transmettre explicitement l'utilisateur au contrôleur principal
            controller.setUser(user);

            // Obtenir la fenêtre actuelle
            Stage stage = (Stage) emailField.getScene().getWindow();

            // Remplacer la scène actuelle par la nouvelle
            stage.getScene().setRoot(root); // C'est la ligne clé pour éviter une nouvelle fenêtre

            // Optionnel: ajuster la taille si nécessaire
            stage.setMaximized(true);

        } catch (IOException e) {
            showError("Erreur lors du chargement de l'interface utilisateur");
            e.printStackTrace();
        }
    }

    @FXML
    private void handleForgotPassword() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/views/ForgotPassword.fxml"));
            Parent root = loader.load();

            // Injectez le service ici
            ForgotPasswordController controller = loader.getController();
            controller.setAuthService(this.authService); // Utilisez le même authService que LoginController

            // Remplacez la scène actuelle
            Scene currentScene = emailField.getScene();
            Stage currentStage = (Stage) currentScene.getWindow();

            currentScene.setRoot(root);
            currentStage.setTitle("Mot de passe oublié");

        } catch (IOException e) {
            AlertUtils.showErrorAlert("Erreur", "Impossible d'ouvrir la page de réinitialisation");
            e.printStackTrace();
        }
    }

    @FXML
    private void handleGoogleLogin() {
        // Implémentation de la connexion Google
        AlertUtils.showInfoAlert("Information", "Google login will be implemented soon");
    }

    @FXML
    private void handleFacebookLogin() {
        // Implémentation de la connexion Facebook
        AlertUtils.showInfoAlert("Information", "Facebook login will be implemented soon");
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }


    @FXML
    private void handleSignUp() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/views/registrationChoice.fxml"));
            Parent root = loader.load();

            // Injectez le service si nécessaire
            // RegistrationChoiceController controller = loader.getController();
            // controller.setAuthService(this.authService);

            // Remplacez la scène actuelle
            Scene currentScene = emailField.getScene();
            Stage currentStage = (Stage) currentScene.getWindow();

            currentScene.setRoot(root);
            currentStage.setTitle("Choix d'inscription");

        } catch (IOException e) {
            AlertUtils.showErrorAlert("Erreur", "Impossible d'ouvrir la page d'inscription");
            e.printStackTrace();
        }
    }

}