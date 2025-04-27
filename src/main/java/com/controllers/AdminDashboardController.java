package com.controllers;

import com.controllers.nour.EditProductController;
import com.models.Produit;
import com.services.AuthService;
import com.services.ProduitServices;
import com.services.UserService;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import javafx.scene.image.ImageView;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

import java.io.File;
import java.sql.SQLException;
import java.util.Comparator;

import javafx.scene.image.Image;
import javafx.scene.control.Button;

public class AdminDashboardController {
    @FXML private VBox sidebar;
    @FXML private StackPane contentPane;
    @FXML private VBox productMenu;
    @FXML private VBox userMenu;

    // Correction: Utiliser les mêmes noms que dans le FXML
    @FXML private Label totalUsersLabel;
    @FXML private Label totalDoctorsLabel;
    @FXML private Label totalPatientsLabel;
    @FXML private VBox appointmentMenu;

    @FXML
    private FlowPane productContainer;

    @FXML
    private ComboBox<String> sortComboBox;


    private List<Produit> currentProducts;

    private String token;
    private AuthService authService = new AuthService();
    private UserService userService = new UserService();

    public void setToken(String token) {
        this.token = token;
        loadStats(); // Charger les stats quand le token est défini
    }

    @FXML
    private void initialize() {

        loadStats(); // Charger les stats à l'initialisation
        // Initialize sorting options
        sortComboBox.setItems(FXCollections.observableArrayList(
                "Default",
                "Name (A-Z)",
                "Name (Z-A)",
                "Price (Low-High)",
                "Price (High-Low)"
        ));
        sortComboBox.setValue("Default");

        // Add listener for sorting
        sortComboBox.setOnAction(event -> handleSort());

        // Load products
        loadProductsInCardView();
    }

    private void loadStats() {
        try {
            int totalUsers = userService.countTotalUsers();
            int totalDoctors = userService.countTotalMedecins();
            int totalPatients = userService.countTotalPatients();

            // Mettre à jour les labels
            if (totalUsersLabel != null) totalUsersLabel.setText(String.valueOf(totalUsers));
            if (totalDoctorsLabel != null) totalDoctorsLabel.setText(String.valueOf(totalDoctors));
            if (totalPatientsLabel != null) totalPatientsLabel.setText(String.valueOf(totalPatients));
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement des statistiques: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void toggleSidebar() {
        sidebar.setVisible(!sidebar.isVisible());
    }

    @FXML
    private void showDashboard() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/views/AdminDashboard.fxml"));
            Parent content = loader.load();

            // Obtenir la référence au contrôleur
            AdminDashboardController controller = loader.getController();


            // Mettre à jour les statistiques
            controller.loadStats();

            // Remplacer le contenu
            contentPane.getChildren().setAll(content);
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible de charger le dashboard", e.getMessage());
        }
    }

    @FXML
    private void showAddProduct() {
        loadContent("/com/views/nour/ListProduit.fxml");
    }

    @FXML
    private void showDoctors() {
        loadContent("/com/views/ListMedecin.fxml");
    }

    @FXML
    private void showPatients() {
        loadContent("/com/views/ListPatient.fxml");
    }

    @FXML
    private void showAddDoctor() {
        loadContent("/com/views/AjouterMedecin.fxml");
    }

    @FXML
    private void showAddPatient() {
        loadContent("/com/views/AjouterPatient.fxml");
    }

    @FXML
    private void showProfile() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/views/profile.fxml"));
            Parent content = loader.load();

            ProfileController profileController = loader.getController();
            profileController.setToken(this.token);

            contentPane.getChildren().setAll(content);
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible de charger le profil", e.getMessage());
        }
    }

    private void showAlert(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    @FXML
    private void logout() {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Logout");
        confirmation.setHeaderText("Confirm Logout");
        confirmation.setContentText("Are you sure you want to logout?");

        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                authService.logout(token);
                redirectToLogin();
            } catch (Exception e) {
                showAlert("Error", "Logout failed", e.getMessage());
            }
        }
    }

    private void redirectToLogin() {
        try {
            Stage currentStage = (Stage) sidebar.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/views/Login.fxml"));
            Parent root = loader.load();

            LoginController loginController = loader.getController();
            loginController.setAuthService(authService);

            Scene scene = new Scene(root);
            currentStage.setScene(scene);
            currentStage.setTitle("Login");
            currentStage.centerOnScreen();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Redirection failed", "Could not load login page: " + e.getMessage());
        }
    }

    private void loadContent(String fxmlPath) {
        try {
            Parent content = FXMLLoader.load(getClass().getResource(fxmlPath));
            contentPane.getChildren().setAll(content);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void toggleProductMenu() {
        productMenu.setVisible(!productMenu.isVisible());
        productMenu.setManaged(!productMenu.isManaged());
    }

    @FXML
    private void toggleUserMenu() {
        userMenu.setVisible(!userMenu.isVisible());
        userMenu.setManaged(!userMenu.isManaged());
    }


    // Ajoutez cette méthode pour gérer le toggle du menu
    @FXML
    private void toggleAppointmentMenu() {
        appointmentMenu.setVisible(!appointmentMenu.isVisible());
        appointmentMenu.setManaged(!appointmentMenu.isManaged());
    }

    // Ajoutez cette méthode pour afficher la gestion des RDV
    @FXML
    private void showAppointmentManagement() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/views/ajout.fxml"));
            Parent content = loader.load();

            // Si vous avez besoin de passer des données au contrôleur
            // AjoutController controller = loader.getController();
            // controller.setSomeData(data);

            contentPane.getChildren().setAll(content);
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Could not load appointment management", e.getMessage());
        }
    }

    // Optionnel: Méthode pour créer un nouveau RDV
    @FXML
    private void showCreateAppointment() {
        // Implémentez la logique pour créer un nouveau RDV
        // Par exemple, ouvrir une nouvelle fenêtre ou charger un autre FXML
    }

    /**
     * Loads and displays the Ajout view in the content pane
     */
    @FXML
    private void showAjoutView() {
        try {
            // Load the Ajout.fxml file
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/views/ajout.fxml"));
            Parent ajoutView = loader.load();

            // Clear the current content and add the new view
            contentPane.getChildren().clear();
            contentPane.getChildren().add(ajoutView);

            // Optional: Get the controller to initialize data if needed
            // Ajout ajoutController = loader.getController();
            // ajoutController.initialize() or any other method

        } catch (IOException e) {
            e.printStackTrace();
            // Handle exception (show error dialog, etc.)
        }
    }



    private void handleSort() {
        if (currentProducts == null || currentProducts.isEmpty()) return;

        String sortOption = sortComboBox.getValue();
        switch (sortOption) {
            case "Name (A-Z)":
                currentProducts.sort(Comparator.comparing(Produit::getName));
                break;
            case "Name (Z-A)":
                currentProducts.sort(Comparator.comparing(Produit::getName).reversed());
                break;
            case "Price (Low-High)":
                currentProducts.sort(Comparator.comparing(Produit::getPrice));
                break;
            case "Price (High-Low)":
                currentProducts.sort(Comparator.comparing(Produit::getPrice).reversed());
                break;
            default:
                // Default order (by ID or as returned from database)
                currentProducts.sort(Comparator.comparing(Produit::getId));
                break;
        }

        // Refresh the view with sorted products
        displayProducts(currentProducts);
    }

    private void loadProductsInCardView() {
        try {
            ProduitServices produitService = new ProduitServices();
            currentProducts = produitService.showAll();
            displayProducts(currentProducts);
        } catch (SQLException e) {
            showAlert("Error", "Failed to load products: " + e.getMessage());
        }
    }

    private void displayProducts(List<Produit> products) {
        productContainer.getChildren().clear();
        for (Produit product : products) {
            VBox card = createProductCard(product);
            productContainer.getChildren().add(card);
        }
    }

    private VBox createProductCard(Produit product) {
        // Create a styled card container
        VBox card = new VBox(10);
        card.setPrefWidth(200);
        card.setPrefHeight(320);
        card.setStyle("-fx-background-color: white; " +
                "-fx-border-color: black; " +
                "-fx-border-width: 1.5; " +  // This sets a 2-pixel border width
                "-fx-border-radius: 8; " +
                "-fx-background-radius: 8; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 5, 0, 0, 2);");

        // Product Image with better styling
        ImageView imageView = new ImageView();
        imageView.setFitWidth(180);
        imageView.setFitHeight(180);
        imageView.setPreserveRatio(true);
        imageView.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 2, 0, 0, 1);");
        loadProductImage(product, imageView);

        // Create container for image to center it
        HBox imageContainer = new HBox(imageView);
        imageContainer.setStyle("-fx-alignment: center; -fx-padding: 10 0 5 0;");

        // Product Details with styled labels
        Label nameLabel = new Label(product.getName());
        nameLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2c3e50; -fx-padding: 0 10 0 10;");
        nameLabel.setWrapText(true);

        Label priceLabel = new Label(String.format("$%.2f", product.getPrice()));
        priceLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #16a085; -fx-padding: 0 10 5 10;");

        // Create container for buttons
        HBox buttonBox = new HBox(10);
        buttonBox.setStyle("-fx-alignment: center; -fx-padding: 5 10 10 10;");

        // Edit Button - Updated styling
        Button editButton = new Button("Edit");
        editButton.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-background-radius: 4;");
        editButton.setOnAction(e -> openEditForm(product));

        // Delete Button
        Button deleteButton = new Button("Delete");
        deleteButton.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-background-radius: 4;");
        deleteButton.setOnAction(e -> deleteProduct(product));

        // Add buttons to button container
        buttonBox.getChildren().addAll(editButton, deleteButton);

        // Add all components to card
        card.getChildren().addAll(imageContainer, nameLabel, priceLabel, buttonBox);
        return card;
    }

    private void deleteProduct(Produit product) {
        try {
            ProduitServices produitService = new ProduitServices();
            produitService.delete(product);
            loadProductsInCardView(); // Refresh the view
            showAlert("Success", "Product deleted successfully");
        } catch (SQLException e) {
            showAlert("Error", "Failed to delete product: " + e.getMessage());
        }
    }

    private void openEditForm(Produit product) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("com/view/nour/edit_product.fxml"));
            Parent root = loader.load();

            EditProductController controller = loader.getController();
            controller.setProductToEdit(product);

            Stage stage = (Stage) productContainer.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Edit Product");

        } catch (Exception e) {
            System.err.println("CRITICAL ERROR OPENING EDIT FORM:");
            e.printStackTrace();

            showAlert("Critical Error",
                    "Cannot open edit form:\n" +
                            e.getClass().getSimpleName() + ": " + e.getMessage() +
                            "\n\nCheck console for details");
        }
    }

    private void loadProductImage(Produit product, ImageView imageView) {
        try {
            String imagePath = product.getImagePath();

            if (imagePath == null || imagePath.isEmpty()) {
                setPlaceholderImage(imageView);
                return;
            }

            Image image;
            if (imagePath.startsWith("/")) {
                image = new Image(getClass().getResourceAsStream(imagePath));
            } else if (imagePath.startsWith("file:")) {
                image = new Image(imagePath);
            } else {
                try {
                    image = new Image(getClass().getResourceAsStream("/images/" + imagePath));
                } catch (Exception e) {
                    image = new Image(new File(imagePath).toURI().toString());
                }
            }

            imageView.setImage(image);
        } catch (Exception e) {
            System.err.println("Error loading image for product: " + product.getName());
            e.printStackTrace();
            setPlaceholderImage(imageView);
        }
    }

    private void setPlaceholderImage(ImageView imageView) {
        try {
            Image placeholder = new Image(getClass().getResourceAsStream("/images/placeholder.png"));
            imageView.setImage(placeholder);
        } catch (Exception e) {
            // Ultimate fallback - blank image
            imageView.setImage(null);
        }
    }

    @FXML
    private void handleTableViewButton() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/views/nour/listProduit.fxml"));
            Stage stage = (Stage) productContainer.getScene().getWindow();
            stage.setScene(new Scene(loader.load()));
            stage.setTitle("Product Table View");
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to load table view");
        }
    }

    @FXML
    private void handleFrontView() {
        try {
            java.net.URL resourceUrl = getClass().getResource("/com/views/nour/product_client.fxml");

            if (resourceUrl == null) {
                showAlert("Error", "Could not find resource: /product_client.fxml");
                return;
            }

            FXMLLoader loader = new FXMLLoader(resourceUrl);
            Parent root = loader.load();

            // Get the current stage
            Stage stage = (Stage) productContainer.getScene().getWindow();

            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle("Front View");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to load front view: " + e.getMessage() +
                    "\nResource URL: " + getClass().getResource("com/views/nour/product_client.fxml"));
        }
    }

    @FXML
    private void handleCategories() {
        handleViewCategories();
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    private void handleStatistics() {
        try {
            java.net.URL resourceUrl = getClass().getResource("/com/views/nour/statistics_view.fxml");

            if (resourceUrl == null) {
                showAlert("Error", "Could not find resource: /com/views/nour/statistics_view.fxml");
                return;
            }

            FXMLLoader loader = new FXMLLoader(resourceUrl);
            Parent root = loader.load();

            Stage statisticsStage = new Stage();
            statisticsStage.setScene(new Scene(root));
            statisticsStage.setTitle("Product Statistics");
            statisticsStage.initModality(Modality.APPLICATION_MODAL); // Make it modal
            statisticsStage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to load statistics view: " + e.getMessage() +
                    "\nResource URL: " + getClass().getResource("/com/views/nour/statistics_view.fxml"));
        }
    }

    @FXML
    private void handleDataHistory() {
        try {
            java.net.URL resourceUrl = getClass().getResource("/com/views/nour/data_history_view.fxml");

            if (resourceUrl == null) {
                showAlert("Error", "Could not find resource: /com/views/nour/data_history_view.fxml");
                return;
            }

            FXMLLoader loader = new FXMLLoader(resourceUrl);
            Parent root = loader.load();

            Stage historyStage = new Stage();
            historyStage.setScene(new Scene(root));
            historyStage.setTitle("Data History");
            historyStage.initModality(Modality.APPLICATION_MODAL); // Make it modal
            historyStage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to load data history view: " + e.getMessage() +
                    "\nResource URL: " + getClass().getResource("/com/views/nour/data_history_view.fxml"));
        }
    }

    @FXML
    private void handleViewCategories() {
        try {
            // Get the resource URL to verify it exists
            java.net.URL resourceUrl = getClass().getResource("/com/views/nour/view_categories.fxml");

            if (resourceUrl == null) {
                showAlert("Error", "Could not find resource: /com/views/nour/view_categories.fxml");
                return;
            }

            FXMLLoader loader = new FXMLLoader(resourceUrl);
            Parent root = loader.load();

            // Get the current stage
            Stage stage = (Stage) productContainer.getScene().getWindow();

            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle("View Categories");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to load categories view: " + e.getMessage());
        }
    }
    @FXML
    private void handleAddCategory() {
        try {
            java.net.URL resourceUrl = getClass().getResource("/com/views/nour/add_category.fxml");

            if (resourceUrl == null) {
                showAlert("Error", "Could not find resource: /com/views/nour/add_category.fxml");
                return;
            }

            FXMLLoader loader = new FXMLLoader(resourceUrl);
            Parent root = loader.load();

            Stage stage = (Stage) productContainer.getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle("Add Category");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to load add category view: " + e.getMessage());
        }
    }
    @FXML
    private void handleFavoritesStatistics() {
        try {
            java.net.URL resourceUrl = getClass().getResource("/com/views/nour/favorites_statistics_view.fxml");

            if (resourceUrl == null) {
                showAlert("Error", "Could not find resource: /favorites_statistics_view.fxml");
                return;
            }

            FXMLLoader loader = new FXMLLoader(resourceUrl);
            Parent root = loader.load();

            // Get the current stage and navigate to the statistics view
            Stage stage = (Stage) productContainer.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Favorites Statistics");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to load favorites statistics view: " + e.getMessage());
        }
    }

    public void showProducts(ActionEvent actionEvent) {
    }


}