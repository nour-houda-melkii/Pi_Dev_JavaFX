package controller;

import entity.Reclamation;
import entity.Reponse;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import services.ReclamationServices;
import services.ReponseService;
import services.NotificationService;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.common.PDRectangle;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.ArrayList;
import java.util.stream.Collectors;

public class ReclamationViewController {
    @FXML
    private FlowPane cardsContainer;
    @FXML
    private ComboBox<String> typeFilter;
    @FXML
    private ComboBox<String> statusFilter;
    @FXML
    private TextField searchField;
    @FXML
    private Button exportPdfBtn; // Bouton pour l'export PDF
    @FXML
    private Button statsButton; // Bouton pour les statistiques

    private final ReclamationServices reclamationService = new ReclamationServices();
    private final ReponseService reponseService = new ReponseService();
    private final ObservableList<Reclamation> data = FXCollections.observableArrayList();

    private MainController mainController;

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
        System.out.println("MainController assigné à ReclamationViewController");
    }

    @FXML
    public void initialize() {
        System.out.println("Initialisation de ReclamationViewController");
        setupFilters();
        loadData();

        if (searchField != null) {
            searchField.textProperty().addListener((observable, oldValue, newValue) -> applyFilters());
        } else {
            System.err.println("ERREUR: searchField est null dans ReclamationViewController");
        }

        // Configuration du bouton d'export PDF
        if (exportPdfBtn != null) {
            exportPdfBtn.setOnAction(e -> exportToPdf());
        } else {
            System.err.println("ERREUR: exportPdfBtn est null dans ReclamationViewController");
        }

        // Configuration du bouton de statistiques
        if (statsButton != null) {
            statsButton.setOnAction(e -> showStatistics());
            System.out.println("Bouton de statistiques configuré avec succès");
        } else {
            System.err.println("ERREUR: statsButton est null dans ReclamationViewController");
        }
    }

    // Remplacez la méthode showStatistics() dans ReclamationViewController.java par celle-ci:

    @FXML
    private void showStatistics() {
        try {
            System.out.println("Ouverture de la fenêtre des statistiques...");

            // Essayer différentes approches pour charger le fichier FXML
            FXMLLoader loader = null;

            // Approche 1: Chemin direct dans le package de la classe courante
            URL url = getClass().getResource("statistics_view.fxml");
            if (url != null) {
                System.out.println("FXML trouvé dans le package courant: " + url);
                loader = new FXMLLoader(url);
            }

            // Approche 2: Chemin à partir de la racine
            if (loader == null) {
                url = getClass().getResource("/statistics_view.fxml");
                if (url != null) {
                    System.out.println("FXML trouvé à la racine: " + url);
                    loader = new FXMLLoader(url);
                }
            }

            // Approche 3: Chercher dans différents répertoires
            if (loader == null) {
                String[] possiblePaths = {
                        "/view/statistics_view.fxml",
                        "/views/statistics_view.fxml",
                        "/fxml/statistics_view.fxml",
                        "/resources/views/statistics_view.fxml",
                        "/resources/fxml/statistics_view.fxml",
                        "/resources/statistics_view.fxml"
                };

                for (String path : possiblePaths) {
                    url = getClass().getResource(path);
                    if (url != null) {
                        System.out.println("FXML trouvé au chemin: " + path);
                        loader = new FXMLLoader(url);
                        break;
                    }
                }
            }

            // Approche 4: Utiliser le ClassLoader
            if (loader == null) {
                url = getClass().getClassLoader().getResource("statistics_view.fxml");
                if (url != null) {
                    System.out.println("FXML trouvé avec ClassLoader: " + url);
                    loader = new FXMLLoader(url);
                }
            }

            // Si aucun chargeur n'a été créé, impossible de trouver le fichier
            if (loader == null) {
                throw new IOException("Le fichier FXML n'a pas été trouvé. Vérifiez que le fichier statistics_view.fxml " +
                        "existe dans un des répertoires de ressources et qu'il est bien inclus dans le build.");
            }

            // Charger la vue des statistiques
            Parent root = loader.load();

            // Créer une nouvelle fenêtre
            Stage statisticsStage = new Stage();
            statisticsStage.setTitle("Statistiques des Réclamations");

            // Configurer la scène
            Scene scene = new Scene(root, 900, 700);

            // Essayer de charger la feuille de style
            try {
                URL cssUrl = getClass().getResource("/styles/statistics-styles.css");
                if (cssUrl != null) {
                    scene.getStylesheets().add(cssUrl.toExternalForm());
                    System.out.println("Feuille de style chargée avec succès depuis: " + cssUrl);
                } else {
                    // Essayer d'autres chemins
                    String[] possibleCssPaths = {
                            "/style/statistics-styles.css",
                            "/css/statistics-styles.css",
                            "/statistics-styles.css",
                            "/resources/styles/statistics-styles.css"
                    };

                    for (String path : possibleCssPaths) {
                        cssUrl = getClass().getResource(path);
                        if (cssUrl != null) {
                            scene.getStylesheets().add(cssUrl.toExternalForm());
                            System.out.println("Feuille de style chargée depuis: " + path);
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("AVERTISSEMENT: Impossible de charger la feuille de style: " + e.getMessage());
                e.printStackTrace();
                // Continuer sans la feuille de style
            }

            statisticsStage.setScene(scene);
            statisticsStage.show();

            System.out.println("Fenêtre des statistiques affichée");

        } catch (IOException e) {
            System.err.println("Erreur lors de l'ouverture de la vue des statistiques: " + e.getMessage());
            e.printStackTrace();
            showAlert("Erreur", "Impossible d'afficher les statistiques: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    // Méthode pour exporter les réclamations en PDF
    @FXML
    private void exportToPdf() {
        try {
            if (data.isEmpty()) {
                // Si pas de données, simplement retourner sans message
                return;
            }

            // Ouvrir le dialogue de sauvegarde de fichier
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Enregistrer le PDF");
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Fichiers PDF", "*.pdf"));
            fileChooser.setInitialFileName("reclamations.pdf");

            File file = fileChooser.showSaveDialog(cardsContainer.getScene().getWindow());

            if (file != null) {
                // Créer le document PDF sans notification ni alerte
                generatePdf(file.getAbsolutePath(), data);

                // Aucune notification ni alerte n'est affichée pour un téléchargement direct
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de l'export PDF: " + e.getMessage());
            e.printStackTrace();
            // Même en cas d'erreur, on n'affiche pas de message pour garder l'interface propre
        }
    }

    // Méthode pour générer le contenu du PDF avec une meilleure gestion des caractères spéciaux
    private void generatePdf(String filePath, List<Reclamation> reclamations) throws IOException {
        try (PDDocument document = new PDDocument()) {
            float yPosition = 750;
            PDPage currentPage = new PDPage(PDRectangle.A4);
            document.addPage(currentPage);

            // Créer le flux de contenu
            PDPageContentStream contentStream = new PDPageContentStream(document, currentPage);

            // Ajouter un titre
            contentStream.beginText();
            contentStream.setFont(PDType1Font.HELVETICA_BOLD, 18);
            contentStream.newLineAtOffset(50, yPosition);
            contentStream.showText("Liste des Réclamations");
            contentStream.endText();
            yPosition -= 20;

            // Ajouter la date d'export
            contentStream.beginText();
            contentStream.setFont(PDType1Font.HELVETICA, 12);
            contentStream.newLineAtOffset(50, yPosition);
            contentStream.showText("Date d'export: " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            contentStream.endText();
            yPosition -= 20;

            // Ajouter une légende pour le nombre de réclamations
            contentStream.beginText();
            contentStream.setFont(PDType1Font.HELVETICA, 11);
            contentStream.newLineAtOffset(50, yPosition);
            contentStream.showText("Nombre total de réclamations: " + reclamations.size());
            contentStream.endText();
            yPosition -= 20;

            // En-têtes de colonne
            contentStream.beginText();
            contentStream.setFont(PDType1Font.HELVETICA_BOLD, 12);
            contentStream.newLineAtOffset(50, yPosition);
            contentStream.showText("Type");
            contentStream.newLineAtOffset(100, 0);
            contentStream.showText("Date");
            contentStream.newLineAtOffset(100, 0);
            contentStream.showText("Médecin");
            contentStream.newLineAtOffset(100, 0);
            contentStream.showText("Description");
            contentStream.endText();

            // Ligne de séparation
            contentStream.moveTo(50, yPosition - 5);
            contentStream.lineTo(550, yPosition - 5);
            contentStream.stroke();

            // Contenu - réclamations
            yPosition -= 20;

            // Définir des couleurs alternées pour les lignes
            boolean alternate = false;

            for (Reclamation reclamation : reclamations) {
                // Vérifier s'il reste assez d'espace sur la page
                if (yPosition < 100) {
                    // Fermer le flux de contenu actuel
                    contentStream.close();

                    // Créer une nouvelle page
                    currentPage = new PDPage(PDRectangle.A4);
                    document.addPage(currentPage);

                    // Créer un nouveau flux de contenu pour la nouvelle page
                    contentStream = new PDPageContentStream(document, currentPage);

                    // Remettre la position verticale au haut de la page
                    yPosition = 750;

                    // Ajouter un en-tête pour la nouvelle page
                    contentStream.beginText();
                    contentStream.setFont(PDType1Font.HELVETICA, 10);
                    contentStream.newLineAtOffset(50, yPosition);
                    contentStream.showText("Liste des Réclamations (suite)");
                    contentStream.endText();
                    yPosition -= 20;

                    // En-têtes de colonne sur la nouvelle page
                    contentStream.beginText();
                    contentStream.setFont(PDType1Font.HELVETICA_BOLD, 10);
                    contentStream.newLineAtOffset(50, yPosition);
                    contentStream.showText("Type");
                    contentStream.newLineAtOffset(100, 0);
                    contentStream.showText("Date");
                    contentStream.newLineAtOffset(100, 0);
                    contentStream.showText("Médecin");
                    contentStream.newLineAtOffset(100, 0);
                    contentStream.showText("Description");
                    contentStream.endText();

                    // Ligne de séparation
                    contentStream.moveTo(50, yPosition - 5);
                    contentStream.lineTo(550, yPosition - 5);
                    contentStream.stroke();

                    yPosition -= 20;
                }

                // Couleur de fond alternée pour les lignes (effet visuel)
                if (alternate) {
                    contentStream.setNonStrokingColor(0.95f, 0.95f, 0.95f);
                    contentStream.addRect(50, yPosition - 3, 500, 15);
                    contentStream.fill();
                    contentStream.setNonStrokingColor(0, 0, 0); // Reset to black
                }
                alternate = !alternate;

                // Type de réclamation
                contentStream.beginText();
                contentStream.setFont(PDType1Font.HELVETICA, 10);
                contentStream.newLineAtOffset(50, yPosition);

                try {
                    // Traiter le texte pour enlever les caractères problématiques
                    contentStream.showText(sanitizeText(reclamation.getTypeReclamationName(), 15));
                } catch (Exception e) {
                    contentStream.showText("[Type]");
                }

                // Date
                contentStream.newLineAtOffset(100, 0);
                try {
                    contentStream.showText(sanitizeText(reclamation.getFormattedDate(), 15));
                } catch (Exception e) {
                    contentStream.showText("[Date]");
                }

                // Médecin
                contentStream.newLineAtOffset(100, 0);
                try {
                    contentStream.showText(sanitizeText(reclamation.getMedecinName(), 15));
                } catch (Exception e) {
                    contentStream.showText("[Médecin]");
                }

                // Description
                contentStream.newLineAtOffset(100, 0);
                try {
                    String description = reclamation.getDescription();
                    if (description != null) {
                        contentStream.showText(sanitizeText(description, 30));
                    } else {
                        contentStream.showText("N/A");
                    }
                } catch (Exception e) {
                    contentStream.showText("[Description non affichable]");
                }

                contentStream.endText();

                yPosition -= 15;
            }

            // Ajouter un pied de page
            contentStream.beginText();
            contentStream.setFont(PDType1Font.HELVETICA, 8); // Remplacé HELVETICA_ITALIC par HELVETICA
            contentStream.newLineAtOffset(50, 30);
            contentStream.showText("Document généré le " +
                    LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) +
                    " à " + java.time.LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));
            contentStream.endText();

            // Pagination
            contentStream.beginText();
            contentStream.setFont(PDType1Font.HELVETICA, 8);
            contentStream.newLineAtOffset(500, 30);
            contentStream.showText("Page " + document.getNumberOfPages());
            contentStream.endText();

            // Fermer le dernier flux de contenu
            contentStream.close();

            // Enregistrer le document
            document.save(filePath);
        }
    }

    // Méthode utilitaire pour nettoyer et tronquer le texte
    private String sanitizeText(String text, int maxLength) {
        if (text == null) return "N/A";

        // Remplacer tous les caractères problématiques
        StringBuilder sb = new StringBuilder();
        for (char c : text.toCharArray()) {
            // Remplacer les caractères non imprimables ou spéciaux
            if (c == '\n' || c == '\r' || c == '\t' || c < 32 || c > 126) {
                sb.append(' ');
            } else {
                sb.append(c);
            }
        }

        String cleanText = sb.toString().trim();

        // Supprimer les espaces multiples
        cleanText = cleanText.replaceAll("\\s+", " ");

        // Tronquer si nécessaire
        return cleanText.length() > maxLength ? cleanText.substring(0, maxLength) + "..." : cleanText;
    }

    private void setupFilters() {
        try {
            if (typeFilter == null) {
                System.err.println("ERREUR: typeFilter est null dans setupFilters()");
                return;
            }

            List<Reclamation> allReclamations = reclamationService.getAllReclamationsWithNames();
            List<String> types = allReclamations.stream()
                    .map(Reclamation::getTypeReclamationName)
                    .distinct()
                    .collect(Collectors.toList());

            typeFilter.getItems().add("Tous les types");
            typeFilter.getItems().addAll(types);
            typeFilter.setValue("Tous les types");
            typeFilter.setOnAction(e -> applyFilters());

            if (statusFilter != null) {
                statusFilter.setVisible(false);
            }
        } catch (SQLException e) {
            System.err.println("Erreur dans setupFilters: " + e.getMessage());
            e.printStackTrace();
            showAlert("Erreur", "Erreur de chargement des types: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    public void refreshReclamations() {
        loadData();
    }

    private void applyFilters() {
        if (typeFilter == null || searchField == null) {
            System.err.println("ERREUR: typeFilter ou searchField est null dans applyFilters()");
            return;
        }

        String selectedType = typeFilter.getValue();
        String searchText = searchField.getText().toLowerCase().trim();

        try {
            List<Reclamation> allReclamations = reclamationService.getAllReclamationsWithNames();
            List<Reclamation> filteredList = new ArrayList<>(allReclamations);

            if (selectedType != null && !"Tous les types".equals(selectedType)) {
                filteredList = filteredList.stream()
                        .filter(r -> selectedType.equalsIgnoreCase(r.getTypeReclamationName()))
                        .collect(Collectors.toList());
            }

            if (!searchText.isEmpty()) {
                filteredList = filteredList.stream()
                        .filter(r ->
                                (r.getDescription() != null && r.getDescription().toLowerCase().contains(searchText)) ||
                                        (r.getFormattedDate() != null && r.getFormattedDate().toLowerCase().contains(searchText)) ||
                                        (r.getMedecinName() != null && r.getMedecinName().toLowerCase().contains(searchText))
                        )
                        .collect(Collectors.toList());
            }

            data.setAll(filteredList);
            displayReclamationsAsCards();
        } catch (SQLException e) {
            System.err.println("Erreur dans applyFilters: " + e.getMessage());
            e.printStackTrace();
            showAlert("Erreur", "Erreur lors du filtrage: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    public void loadData() {
        try {
            data.setAll(reclamationService.getAllReclamationsWithNames());
            displayReclamationsAsCards();
        } catch (SQLException e) {
            System.err.println("Erreur dans loadData: " + e.getMessage());
            e.printStackTrace();
            showAlert("Erreur", "Erreur de chargement: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void displayReclamationsAsCards() {
        if (cardsContainer == null) {
            System.err.println("ERREUR: cardsContainer est null dans displayReclamationsAsCards()");
            return;
        }

        cardsContainer.getChildren().clear();

        for (Reclamation reclamation : data) {
            VBox card = new VBox(10);
            card.getStyleClass().add("reclamation-card");
            card.setStyle("-fx-background-color: white; -fx-border-color: #ddd; -fx-border-radius: 5; " +
                    "-fx-padding: 15; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 5, 0, 0, 5);");
            card.setPrefWidth(300);
            card.setMaxWidth(300);

            HBox header = new HBox();
            header.setAlignment(Pos.CENTER_LEFT);
            header.setSpacing(10);

            Label typeLabel = new Label(reclamation.getTypeReclamationName());
            typeLabel.setStyle("-fx-background-color: #f0f0f0; -fx-padding: 3 8; -fx-background-radius: 4;");

            header.getChildren().add(typeLabel);

            Label dateLabel = new Label("Date: " + reclamation.getFormattedDate());

            Label descriptionTitle = new Label("Description:");
            descriptionTitle.setStyle("-fx-font-weight: bold;");

            TextFlow descriptionFlow = new TextFlow();
            Text descriptionText = new Text(reclamation.getDescription());
            descriptionFlow.getChildren().add(descriptionText);
            descriptionFlow.setStyle("-fx-padding: 5;");

            Label medecinLabel = new Label("Médecin: " + reclamation.getMedecinName());

            HBox actions = new HBox(10);
            actions.setAlignment(Pos.CENTER_RIGHT);

            Button respondBtn = new Button("Répondre");
            respondBtn.setStyle("-fx-background-color: #007bff; -fx-text-fill: white;");
            respondBtn.setOnAction(e -> showResponseDialog(reclamation));

            Button viewResponsesBtn = new Button("Voir réponses");
            viewResponsesBtn.setOnAction(e -> showExistingResponses(reclamation));

            actions.getChildren().addAll(viewResponsesBtn, respondBtn);

            card.getChildren().addAll(
                    header,
                    dateLabel,
                    medecinLabel,
                    descriptionTitle,
                    descriptionFlow,
                    actions
            );

            cardsContainer.getChildren().add(card);
        }
    }

    private void showExistingResponses(Reclamation reclamation) {
        List<Reponse> responses = reponseService.getResponsesForReclamation(reclamation.getId());

        if (responses == null || responses.isEmpty()) {
            showAlert("Information", "Aucune réponse n'a été fournie pour cette réclamation.", Alert.AlertType.INFORMATION);
            return;
        }

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Réponses pour cette réclamation");

        VBox content = new VBox(10);
        content.setPadding(new Insets(20));

        for (Reponse response : responses) {
            VBox responseBox = new VBox(5);
            responseBox.setStyle("-fx-background-color: #f8f9fa; -fx-padding: 10; -fx-border-color: #ddd; -fx-border-radius: 5;");

            Label dateLabel = new Label(response.getDateReponse().toString());
            dateLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #6c757d;");

            Label contentLabel = new Label(response.getContenu());
            contentLabel.setWrapText(true);

            responseBox.getChildren().addAll(dateLabel, contentLabel);
            content.getChildren().add(responseBox);
        }

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(400);

        dialog.getDialogPane().setContent(scrollPane);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        dialog.showAndWait();
    }

    private void showResponseDialog(Reclamation reclamation) {
        Dialog<Reponse> dialog = new Dialog<>();
        dialog.setTitle("Répondre à la réclamation");

        ButtonType saveButtonType = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        VBox grid = new VBox(10);
        grid.setPadding(new Insets(20));

        TextArea responseArea = new TextArea();
        responseArea.setPromptText("Entrez votre réponse ici...");
        responseArea.setPrefRowCount(5);

        grid.getChildren().addAll(new Label("Réponse:"), responseArea);

        List<Reponse> responses = reponseService.getResponsesForReclamation(reclamation.getId());
        if (responses != null && !responses.isEmpty()) {
            VBox responsesBox = new VBox(5);
            responsesBox.getChildren().add(new Label("Réponses existantes:"));

            for (Reponse r : responses) {
                Label responseLabel = new Label(r.getDateReponse() + ": " + r.getContenu());
                responseLabel.setWrapText(true);
                responsesBox.getChildren().add(responseLabel);
            }

            grid.getChildren().add(responsesBox);
        }

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                Reponse reponse = new Reponse();
                reponse.setContenu(responseArea.getText());
                reponse.setDateReponse(LocalDate.now());
                reponse.setReclamationId(reclamation.getId());
                return reponse;
            }
            return null;
        });

        Optional<Reponse> result = dialog.showAndWait();
        result.ifPresent(reponse -> {
            try {
                System.out.println("Enregistrement de la réponse et envoi de notification...");

                // Ajouter la réponse dans la base de données
                reponseService.addReponse(reponse);

                // Préparer le message de notification
                String notificationMessage = "Nouvelle réponse à la réclamation :" +reclamation.getDescription() +" reponse: "+ reponse.getContenu();

                // Envoyer la notification à tous les abonnés (y compris MainController)
                NotificationService.getInstance().sendNotification(notificationMessage);
                System.out.println("Notification envoyée via NotificationService");

                // Rafraîchir l'affichage des réclamations
                loadData();

                // Afficher une confirmation locale
                showAlert("Succès", "Réponse ajoutée avec succès", Alert.AlertType.INFORMATION);

            } catch (Exception e) {
                System.err.println("Erreur lors de l'ajout de la réponse: " + e.getMessage());
                e.printStackTrace();
                showAlert("Erreur", "Échec d'enregistrement: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        });
    }

    @FXML
    private void handleSomeAction() {
        if (mainController != null) {
            mainController.showSimpleNotification("Opération effectuée");
        }
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // Cette méthode peut être utilisée comme un test
    @FXML
    private void testSendNotification() {
        String testMessage = "Test de notification depuis ReclamationViewController: " + System.currentTimeMillis();
        NotificationService.getInstance().sendNotification(testMessage);
        showAlert("Test", "Notification envoyée", Alert.AlertType.INFORMATION);
    }
}