package controllers;

import entities.Produit;
import Services.ProduitServices;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.ComboBox;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.control.Button;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.collections.FXCollections;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.List;

public class AdminDashboardController {
    @FXML
    private FlowPane productContainer;

    @FXML
    private ComboBox<String> sortComboBox;


    private List<Produit> currentProducts;


    @FXML
    public void initialize() {
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
                "-fx-border-color: #e0e0e0; " +
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
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/listproduit.fxml"));
            Parent root = loader.load();

            ListeProduitController controller = loader.getController();
            controller.setProductToEdit(product);

            Stage popupStage = new Stage();
            popupStage.setScene(new Scene(root));
            popupStage.setTitle("Edit Product");
            popupStage.initModality(Modality.APPLICATION_MODAL);
            popupStage.showAndWait();

            // Refresh after editing
            loadProductsInCardView();

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

            // Handle cases where path might be null or empty
            if (imagePath == null || imagePath.isEmpty()) {
                setPlaceholderImage(imageView);
                return;
            }

            // Handle both absolute and relative paths
            Image image;
            if (imagePath.startsWith("/")) {
                // Resource path (from JAR)
                image = new Image(getClass().getResourceAsStream(imagePath));
            } else if (imagePath.startsWith("file:")) {
                // Absolute file path
                image = new Image(imagePath);
            } else {
                // Relative path - try both resource and file system
                try {
                    // First try as resource
                    image = new Image(getClass().getResourceAsStream("/images/" + imagePath));
                } catch (Exception e) {
                    // Fall back to file system
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
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/listproduit.fxml"));
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
            // Get the resource URL to verify it exists
            java.net.URL resourceUrl = getClass().getResource("/product_client.fxml");

            if (resourceUrl == null) {
                showAlert("Error", "Could not find resource: /product_client.fxml");
                return;
            }

            FXMLLoader loader = new FXMLLoader(resourceUrl);
            Parent root = loader.load();

            // Get the current stage
            Stage stage = (Stage) productContainer.getScene().getWindow();

            // Set the new scene
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle("Front View");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to load front view: " + e.getMessage() +
                    "\nResource URL: " + getClass().getResource("/product_client.fxml"));
        }
    }

    @FXML
    private void handleCategories() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/category.fxml"));
            Stage stage = (Stage) productContainer.getScene().getWindow();
            stage.setScene(new Scene(loader.load()));
            stage.setTitle("Manage Categories");
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to load categories view");
        }
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
            // Get the resource URL to verify it exists
            java.net.URL resourceUrl = getClass().getResource("/statistics_view.fxml");

            if (resourceUrl == null) {
                showAlert("Error", "Could not find resource: /statistics_view.fxml");
                return;
            }

            FXMLLoader loader = new FXMLLoader(resourceUrl);
            Parent root = loader.load();

            // Create a new stage for the statistics view
            Stage statisticsStage = new Stage();
            statisticsStage.setScene(new Scene(root));
            statisticsStage.setTitle("Product Statistics");
            statisticsStage.initModality(Modality.APPLICATION_MODAL); // Make it modal
            statisticsStage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to load statistics view: " + e.getMessage() +
                    "\nResource URL: " + getClass().getResource("/statistics_view.fxml"));
        }
    }

    @FXML
    private void handleDataHistory() {
        try {
            // Get the resource URL to verify it exists
            java.net.URL resourceUrl = getClass().getResource("/data_history_view.fxml");

            if (resourceUrl == null) {
                showAlert("Error", "Could not find resource: /data_history_view.fxml");
                return;
            }

            FXMLLoader loader = new FXMLLoader(resourceUrl);
            Parent root = loader.load();

            // Create a new stage for the data history view
            Stage historyStage = new Stage();
            historyStage.setScene(new Scene(root));
            historyStage.setTitle("Data History");
            historyStage.initModality(Modality.APPLICATION_MODAL); // Make it modal
            historyStage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to load data history view: " + e.getMessage() +
                    "\nResource URL: " + getClass().getResource("/data_history_view.fxml"));
        }
    }
}