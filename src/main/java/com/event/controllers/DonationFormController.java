package com.event.controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.scene.web.WebView;
import javafx.scene.web.WebEngine;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.scene.layout.VBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.Priority;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.application.Platform;

import com.event.utils.StripePaymentGateway;
import java.net.URL;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicBoolean;
import javafx.scene.paint.Color;
import javafx.scene.effect.DropShadow;
import com.stripe.exception.StripeException;

public class DonationFormController implements Initializable {
    
    @FXML
    private TextField lastNameField;
    
    @FXML
    private TextField firstNameField;
    
    @FXML
    private TextField amountField;
    
    @FXML
    private ComboBox<String> currencyComboBox;
    
    @FXML
    private CheckBox anonymousCheckbox;
    
    @FXML
    private TextArea messageArea;
    
    @FXML
    private Button cancelButton;
    
    @FXML
    private Button donateButton;
    
    private Stage stage;
    
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Initialiser le combobox des devises
        currencyComboBox.getItems().addAll("EUR", "USD", "GBP");
        currencyComboBox.setValue("EUR");
        
        // Limiter la saisie dans le champ montant à des nombres
        amountField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*(\\.\\d{0,2})?")) {
                amountField.setText(oldValue);
            }
        });
        
        // Observer l'état de la case à cocher "Anonyme"
        anonymousCheckbox.selectedProperty().addListener((observable, oldValue, newValue) -> {
            // Désactiver les champs nom/prénom si anonyme est coché
            lastNameField.setDisable(newValue);
            firstNameField.setDisable(newValue);
            
            if (newValue) {
                // Sauvegarder les valeurs précédentes
                lastNameField.getProperties().put("savedValue", lastNameField.getText());
                firstNameField.getProperties().put("savedValue", firstNameField.getText());
                
                // Effacer les champs
                lastNameField.clear();
                firstNameField.clear();
            } else {
                // Restaurer les valeurs précédentes
                if (lastNameField.getProperties().containsKey("savedValue")) {
                    lastNameField.setText((String) lastNameField.getProperties().get("savedValue"));
                }
                if (firstNameField.getProperties().containsKey("savedValue")) {
                    firstNameField.setText((String) firstNameField.getProperties().get("savedValue"));
                }
            }
        });
    }
    
    /**
     * Définit la référence à la fenêtre pour pouvoir la fermer
     */
    public void setStage(Stage stage) {
        this.stage = stage;
    }
    
    /**
     * Gère le clic sur le bouton Annuler
     */
    @FXML
    private void handleCancel() {
        if (stage != null) {
            stage.close();
        }
    }
    
    /**
     * Gère le clic sur le bouton Procéder au paiement
     */
    @FXML
    private void handleDonate() {
        if (validateForm()) {
            try {
                // Afficher un indicateur de chargement
                donateButton.setDisable(true);
                donateButton.setText("Traitement en cours...");
                
                // Récupérer les valeurs du formulaire
                String lastName = anonymousCheckbox.isSelected() ? "" : lastNameField.getText().trim();
                String firstName = anonymousCheckbox.isSelected() ? "" : firstNameField.getText().trim();
                String donorName = anonymousCheckbox.isSelected() ? "Anonyme" : firstName + " " + lastName;
                
                // Convertir le montant en centimes
                double amountValue = NumberFormat.getInstance().parse(amountField.getText()).doubleValue();
                int amountInCents = (int) (amountValue * 100);
                
                String currency = currencyComboBox.getValue();
                String message = messageArea.getText().trim();
                
                // Description pour le paiement
                String description = "Don" + (message.isEmpty() ? "" : " - " + message);
                
                // Créer une session de paiement Stripe (opération qui peut prendre du temps)
                Thread stripeThread = new Thread(() -> {
                    try {
                        // Appeler l'API Stripe
                        String paymentUrl = StripePaymentGateway.createPaymentSession(
                            amountInCents, 
                            currency, 
                            description, 
                            donorName
                        );
                        
                        // Afficher la passerelle de paiement dans une WebView sur le thread UI
                        Platform.runLater(() -> {
                            donateButton.setDisable(false);
                            donateButton.setText("Procéder au paiement");
                            showStripePaymentPage(paymentUrl);
                        });
                    } catch (StripeException e) {
                        Platform.runLater(() -> {
                            donateButton.setDisable(false);
                            donateButton.setText("Procéder au paiement");
                            
                            // Afficher l'erreur
                            String errorMsg = "Erreur Stripe: " + e.getMessage();
                            if (e.getStripeError() != null && e.getStripeError().getMessage() != null) {
                                errorMsg = e.getStripeError().getMessage();
                            }
                            
                            showAlert(Alert.AlertType.ERROR, "Erreur de paiement", 
                                "Une erreur est survenue lors de la connexion à Stripe: " + errorMsg);
                        });
                    }
                });
                
                stripeThread.setDaemon(true);
                stripeThread.start();
                
            } catch (ParseException e) {
                donateButton.setDisable(false);
                donateButton.setText("Procéder au paiement");
                showAlert(Alert.AlertType.ERROR, "Erreur de saisie", 
                    "Le montant saisi n'est pas valide.");
            } catch (Exception e) {
                donateButton.setDisable(false);
                donateButton.setText("Procéder au paiement");
                showAlert(Alert.AlertType.ERROR, "Erreur", 
                    "Une erreur est survenue lors du traitement du paiement : " + e.getMessage());
            }
        }
    }
    
    /**
     * Valide les champs du formulaire
     */
    private boolean validateForm() {
        // Vérifier si le montant est valide
        if (amountField.getText().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Champ requis", 
                "Veuillez saisir un montant pour votre don.");
            amountField.requestFocus();
            return false;
        }
        
        try {
            double amount = NumberFormat.getInstance().parse(amountField.getText()).doubleValue();
            if (amount <= 0) {
                showAlert(Alert.AlertType.WARNING, "Montant invalide", 
                    "Le montant du don doit être supérieur à 0.");
                amountField.requestFocus();
                return false;
            }
            
            // Stripe n'accepte pas les montants inférieurs à 0.50€
            if (amount < 0.50) {
                showAlert(Alert.AlertType.WARNING, "Montant invalide", 
                    "Le montant minimum accepté par Stripe est de 0.50 " + currencyComboBox.getValue() + ".");
                amountField.requestFocus();
                return false;
            }
        } catch (ParseException e) {
            showAlert(Alert.AlertType.WARNING, "Montant invalide", 
                "Veuillez saisir un montant valide.");
            amountField.requestFocus();
            return false;
        }
        
        // Vérifier si les noms sont saisis (sauf si anonyme)
        if (!anonymousCheckbox.isSelected()) {
            if (lastNameField.getText().trim().isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Champ requis", 
                    "Veuillez saisir votre nom ou cocher l'option de don anonyme.");
                lastNameField.requestFocus();
                return false;
            }
            
            if (firstNameField.getText().trim().isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Champ requis", 
                    "Veuillez saisir votre prénom ou cocher l'option de don anonyme.");
                firstNameField.requestFocus();
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Affiche une alerte avec le type, titre et message spécifiés
     */
    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    /**
     * Affiche la page de paiement Stripe dans une WebView
     */
    private void showStripePaymentPage(String paymentUrl) {
        try {
            // Créer une nouvelle fenêtre modale pour la WebView
            Stage paymentStage = new Stage();
            paymentStage.initModality(Modality.APPLICATION_MODAL);
            paymentStage.setTitle("Paiement sécurisé");
            paymentStage.setMinWidth(800);
            paymentStage.setMinHeight(600);
            
            // Créer la WebView pour afficher la page Stripe
            WebView webView = new WebView();
            WebEngine webEngine = webView.getEngine();
            
            // Créer une barre de progression
            ProgressBar progressBar = new ProgressBar();
            progressBar.setMaxWidth(Double.MAX_VALUE);
            
            // Observer le chargement de la page
            webEngine.getLoadWorker().progressProperty().addListener((obs, oldVal, newVal) -> {
                progressBar.setProgress(newVal.doubleValue());
                if (newVal.doubleValue() == 1.0) {
                    progressBar.setVisible(false);
                }
            });
            
            // Observer l'URL pour détecter les redirections success/cancel
            AtomicBoolean paymentHandled = new AtomicBoolean(false);
            
            webEngine.locationProperty().addListener((obs, oldUrl, newUrl) -> {
                System.out.println("URL changed: " + newUrl);
                if (!paymentHandled.get() && 
                   (newUrl.contains("/success") || newUrl.contains("checkout.stripe.success") || 
                    newUrl.contains("/cancel") || newUrl.contains("checkout.stripe.cancel"))) {
                    
                    paymentHandled.set(true);
                    
                    if (newUrl.contains("/success") || newUrl.contains("checkout.stripe.success")) {
                        // Paiement réussi
                        Platform.runLater(() -> {
                            paymentStage.close();
                            showThankYouPopup();
                        });
                    } else {
                        // Paiement annulé
                        Platform.runLater(() -> {
                            paymentStage.close();
                            showAlert(Alert.AlertType.INFORMATION, "Paiement annulé", 
                                "Le paiement a été annulé. Aucun montant n'a été débité.");
                        });
                    }
                }
            });
            
            // Charger l'URL de paiement Stripe
            webEngine.load(paymentUrl);
            
            // Mise en page
            VBox root = new VBox(5);
            root.setPadding(new Insets(10));
            root.getChildren().addAll(progressBar, webView);
            VBox.setVgrow(webView, Priority.ALWAYS);
            
            // Bouton de fermeture
            Button closeButton = new Button("Fermer");
            closeButton.setOnAction(e -> paymentStage.close());
            
            StackPane bottomPane = new StackPane(closeButton);
            bottomPane.setPadding(new Insets(10, 0, 0, 0));
            bottomPane.setAlignment(Pos.CENTER_RIGHT);
            
            root.getChildren().add(bottomPane);
            
            // Configurer et afficher la scène
            Scene scene = new Scene(root);
            paymentStage.setScene(scene);
            paymentStage.show();
            
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", 
                "Impossible d'afficher la page de paiement: " + e.getMessage());
        }
    }
    
    /**
     * Affiche une fenêtre popup de remerciement après un paiement réussi
     */
    private void showThankYouPopup() {
        try {
            // Créer une nouvelle fenêtre
            Stage thankYouStage = new Stage();
            thankYouStage.initModality(Modality.APPLICATION_MODAL);
            thankYouStage.setTitle("Merci pour votre don");
            thankYouStage.setResizable(false);
            
            // Créer les éléments UI pour la popup
            VBox root = new VBox(15);
            root.setAlignment(Pos.CENTER);
            root.setPadding(new Insets(30));
            root.setMinWidth(400);
            root.setMaxWidth(400);
            root.setStyle("-fx-background-color: white; -fx-background-radius: 10px;");
            
            // Titre
            Label titleLabel = new Label("Merci pour votre don !");
            titleLabel.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #4568dc;");
            
            // Icône de succès
            Label iconLabel = new Label("✓");
            iconLabel.setStyle(
                "-fx-font-size: 50px; " +
                "-fx-text-fill: white; " +
                "-fx-background-color: #4CAF50; " +
                "-fx-background-radius: 50%; " +
                "-fx-padding: 10px 20px; " +
                "-fx-alignment: center;"
            );
            
            // Message de remerciement
            Label messageLabel = new Label("Votre paiement a été traité avec succès. Nous vous remercions pour votre générosité et votre soutien à notre cause.");
            messageLabel.setWrapText(true);
            messageLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #555;");
            messageLabel.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
            
            // Montant et informations supplémentaires
            String currency = currencyComboBox.getValue();
            String amountStr = amountField.getText();
            Label amountLabel = new Label("Montant: " + amountStr + " " + currency);
            amountLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
            
            // Bouton de fermeture
            Button closeButton = new Button("Fermer");
            closeButton.setStyle(
                "-fx-background-color: #4568dc; " +
                "-fx-text-fill: white; " +
                "-fx-padding: 10px 30px; " +
                "-fx-cursor: hand; " +
                "-fx-font-size: 14px; " +
                "-fx-background-radius: 5px;"
            );
            closeButton.setOnAction(e -> {
                thankYouStage.close();
                // Fermer aussi la fenêtre de don si elle est encore ouverte
                if (stage != null) {
                    stage.close();
                }
            });
            
            // Effet de survol pour le bouton
            closeButton.setOnMouseEntered(e -> 
                closeButton.setStyle(
                    "-fx-background-color: #3052c7; " +
                    "-fx-text-fill: white; " +
                    "-fx-padding: 10px 30px; " +
                    "-fx-cursor: hand; " +
                    "-fx-font-size: 14px; " +
                    "-fx-background-radius: 5px;"
                )
            );
            closeButton.setOnMouseExited(e -> 
                closeButton.setStyle(
                    "-fx-background-color: #4568dc; " +
                    "-fx-text-fill: white; " +
                    "-fx-padding: 10px 30px; " +
                    "-fx-cursor: hand; " +
                    "-fx-font-size: 14px; " +
                    "-fx-background-radius: 5px;"
                )
            );
            
            // Ajouter tous les éléments au conteneur
            root.getChildren().addAll(titleLabel, iconLabel, messageLabel, amountLabel, closeButton);
            
            // Ajouter un effet d'ombre
            root.setEffect(new DropShadow(10, Color.gray(0.5, 0.5)));
            
            // Configurer la scène avec un fond légèrement transparent
            StackPane container = new StackPane(root);
            container.setStyle("-fx-background-color: rgba(0, 0, 0, 0.1);");
            container.setPadding(new Insets(20));
            
            Scene scene = new Scene(container);
            thankYouStage.setScene(scene);
            
            // Centrer la fenêtre et l'afficher
            thankYouStage.centerOnScreen();
            thankYouStage.showAndWait();
            
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", 
                "Impossible d'afficher le message de remerciement: " + e.getMessage());
        }
    }
} 