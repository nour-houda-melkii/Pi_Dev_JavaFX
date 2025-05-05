package com.controllers.nour;

import com.controllers.nour.ProductFrontController;
import com.models.*;
import com.services.FavoriServices; // Add this import
import com.utils.AuthManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert; // Add this import
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException; // Add this import
import java.util.List;
import java.util.logging.Level; // Add this import
import java.util.logging.Logger; // Add this import

public class FavoritesViewController {
    // Add a logger
    private static final Logger LOGGER = Logger.getLogger(FavoritesViewController.class.getName());

    @FXML
    private FlowPane favoritesContainer;

    @FXML
    private Label totalItemsLabel;

    private List<Produit> favoriteProducts;
    private int currentUserId;

    // Add FavoriServices
    private FavoriServices favoriService;

    @FXML
    public void initialize() {
        // Initialize FavoriServices
        favoriService = new FavoriServices();

        // Set spacing and styling for the favorites container
        favoritesContainer.setHgap(20);
        favoritesContainer.setVgap(20);
        favoritesContainer.setPrefWidth(800);
        favoritesContainer.setStyle("-fx-background-color: #f8f9fa; -fx-padding: 20;");
    }

    public void setFavoriteProducts(List<Produit> favoriteProducts) {
        this.favoriteProducts = favoriteProducts;
        displayFavorites();
        updateCounter();
    }


    private void displayFavorites() {
        favoritesContainer.getChildren().clear();

        if (favoriteProducts.isEmpty()) {
            VBox emptyBox = new VBox();
            emptyBox.setStyle("-fx-background-color: white; " +
                    "-fx-border-color: #000000; " +
                    "-fx-border-width: 2; " +
                    "-fx-border-radius: 10; " +
                    "-fx-background-radius: 10; " +
                    "-fx-padding: 40; " +
                    "-fx-alignment: center;");
            emptyBox.setPrefWidth(600);
            emptyBox.setPrefHeight(200);

            Label emptyIcon = new Label("♡");
            emptyIcon.setStyle("-fx-font-size: 40px; -fx-text-fill: #dc3545;");

            Label emptyLabel = new Label("You don't have any favorite products yet");
            emptyLabel.setStyle("-fx-font-size: 18px; -fx-text-fill: #495057; -fx-padding: 10;");

            Button browseButton = new Button("Browse Products");
            browseButton.setStyle("-fx-background-color: #007bff; " +
                    "-fx-text-fill: white; " +
                    "-fx-border-color: #000000; " +
                    "-fx-border-width: 1; " +
                    "-fx-border-radius: 20; " +
                    "-fx-background-radius: 20; " +
                    "-fx-font-size: 14px; " +
                    "-fx-padding: 10 20;");
            browseButton.setOnAction(e -> handleBackToProducts());

            emptyBox.getChildren().addAll(emptyIcon, emptyLabel, browseButton);
            favoritesContainer.getChildren().add(emptyBox);
        } else {
            for (Produit product : favoriteProducts) {
                favoritesContainer.getChildren().add(createProductCard(product));
            }
        }
    }

    private VBox createProductCard(Produit product) {
        // Create a styled card container with black border
        VBox card = new VBox(10);
        card.setPrefWidth(240);
        card.setPrefHeight(400);
        card.setStyle("-fx-background-color: white; " +
                "-fx-border-color: #000000; " +
                "-fx-border-width: 2; " +
                "-fx-border-radius: 10; " +
                "-fx-background-radius: 10;");

        // Apply drop shadow effect
        DropShadow shadow = new DropShadow();
        shadow.setColor(Color.color(0, 0, 0, 0.3));
        shadow.setRadius(10);
        shadow.setOffsetX(0);
        shadow.setOffsetY(2);
        card.setEffect(shadow);

        // Product Image
        ImageView imageView = new ImageView();
        imageView.setFitWidth(200);
        imageView.setFitHeight(200);
        imageView.setPreserveRatio(true);
        loadProductImage(product, imageView);

        // Image border
        imageView.setStyle("-fx-border-color: #000000; -fx-border-width: 1;");

        // Image container with padding
        HBox imageContainer = new HBox(imageView);
        imageContainer.setStyle("-fx-alignment: center; -fx-padding: 15 0 10 0;");

        // Product Details Section - in a VBox with border
        VBox detailsContainer = new VBox(8);
        detailsContainer.setStyle("-fx-background-color: #f8f9fa; " +
                "-fx-border-color: #000000; " +
                "-fx-border-width: 1 0; " +
                "-fx-padding: 10;");

        // Product name
        Label nameLabel = new Label(product.getName());
        nameLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #212529;");
        nameLabel.setWrapText(true);
        nameLabel.setMaxWidth(200);

        // Price with currency symbol
        Label priceLabel = new Label("" + String.format("%.2f", product.getPrice()));
        priceLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #28a745; -fx-font-weight: bold;");

        // Description with scrollable area if too long
        Label descLabel = new Label(product.getDesciption());
        descLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #6c757d;");
        descLabel.setWrapText(true);
        descLabel.setMaxWidth(200);
        descLabel.setMaxHeight(60);

        // Add details to container
        detailsContainer.getChildren().addAll(nameLabel, priceLabel, descLabel);

        // Buttons container
        HBox buttonBox = new HBox(10);
        buttonBox.setStyle("-fx-alignment: center; -fx-padding: 12;");


        // Remove button with enhanced styling
        Button removeButton = new Button("♥ Remove");
        removeButton.setStyle("-fx-background-color: #dc3545; " +
                "-fx-text-fill: white; " +
                "-fx-border-color: #000000; " +
                "-fx-border-width: 1; " +
                "-fx-border-radius: 20; " +
                "-fx-background-radius: 20;");
        removeButton.setOnAction(e -> {
            // Call the new method to remove from database and update local list
            removeFavorite(product);
        });

        // Add buttons to container
        buttonBox.getChildren().addAll(removeButton);

        // Add all components to card
        card.getChildren().addAll(imageContainer, detailsContainer, buttonBox);

        return card;
    }

    // New method to handle favorite removal from database
    private void removeFavorite(Produit product) {
        try {
            // Create a Favori object with the currentUserId and product ID
            Favori favori = new Favori(currentUserId, product.getId());

            // Delete from database
            boolean deleted = favoriService.delete(favori);

            if (deleted) {
                // If successfully deleted from database, remove from local list
                favoriteProducts.removeIf(p -> p.getId() == product.getId());
                // Update the UI
                displayFavorites();
                updateCounter();
                // Show success message
                showAlert("Success", "Product removed from favorites successfully!");
            } else {
                // If delete failed, show error
                showAlert("Error", "Failed to remove product from favorites.");
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Error removing favorite", ex);
            showAlert("Error", "Database error: " + ex.getMessage());
        }
    }

    private void loadProductImage(Produit product, ImageView imageView) {
        try {
            if (product.getImagePath() != null && !product.getImagePath().isEmpty()) {
                File file = new File(product.getImagePath());
                if (file.exists()) {
                    imageView.setImage(new Image(file.toURI().toString()));
                } else {
                    loadPlaceholderImage(imageView);
                }
            } else {
                loadPlaceholderImage(imageView);
            }
        } catch (Exception e) {
            loadPlaceholderImage(imageView);
        }
    }

    private void loadPlaceholderImage(ImageView imageView) {
        try {
            imageView.setImage(new Image(getClass().getResourceAsStream("/images/placeholder.png")));
        } catch (Exception ex) {
            // If placeholder can't be loaded, leave it empty
            LOGGER.log(Level.WARNING, "Error loading placeholder image: " + ex.getMessage());
        }
    }

    private void updateCounter() {
        totalItemsLabel.setText(favoriteProducts.size() + " favorite items");
        totalItemsLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #0056b3;");
    }

    // Helper method to show alerts
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    private void handleBackToProducts() {
        try {
            // First, ensure the user ID is stored in AuthManager for backup access
            if (currentUserId > 0) {
                AuthManager.storeUserId(currentUserId);
            }

            // Log auth state before navigation
            AuthManager.logAuthState();

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/views/nour/product_client.fxml"));
            Parent root = loader.load();

            ProductFrontController controller = loader.getController();
            controller.setCurrentUserId(currentUserId);

            Stage stage = (Stage) favoritesContainer.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Products");
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to navigate to products view: " + e.getMessage());
        }
    }

    // Update this method to actually set the currentUserId field
    public void setCurrentUserId(int currentUserId) {
        this.currentUserId = currentUserId;
        LOGGER.info("Current user ID set to: " + currentUserId);
    }
}