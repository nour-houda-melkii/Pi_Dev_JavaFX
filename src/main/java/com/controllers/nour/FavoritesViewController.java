package com.controllers.nour;

import com.models.*;
import com.services.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class FavoritesViewController {
    @FXML
    private FlowPane favoritesContainer;

    @FXML
    private Label totalItemsLabel;

    private List<Produit> favoriteProducts;

    @FXML
    public void initialize() {
        // Will be initialized when products are set
    }

    public void setFavoriteProducts(List<Produit> favoriteProducts) {
        this.favoriteProducts = favoriteProducts;
        displayFavorites();
        updateCounter();
    }

    private void displayFavorites() {
        favoritesContainer.getChildren().clear();

        if (favoriteProducts.isEmpty()) {
            Label emptyLabel = new Label("You don't have any favorite products yet");
            emptyLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #7f8c8d; -fx-padding: 20;");
            favoritesContainer.getChildren().add(emptyLabel);
        } else {
            for (Produit product : favoriteProducts) {
                favoritesContainer.getChildren().add(createProductCard(product));
            }
        }
    }

    private VBox createProductCard(Produit product) {
        // Create a styled card container
        VBox card = new VBox(10);
        card.setPrefWidth(200);
        card.setPrefHeight(350);
        card.setStyle("-fx-background-color: white; " +
                "-fx-border-color: #f0f0f0; " +
                "-fx-border-radius: 8; " +
                "-fx-background-radius: 8; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 3, 0, 0, 1);");

        // Product Image
        ImageView imageView = new ImageView();
        imageView.setFitWidth(180);
        imageView.setFitHeight(180);
        imageView.setPreserveRatio(true);
        imageView.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 2, 0, 0, 1);");
        loadProductImage(product, imageView);

        // Image container
        HBox imageContainer = new HBox(imageView);
        imageContainer.setStyle("-fx-alignment: center; -fx-padding: 10 0 5 0;");

        // Product Details
        Label nameLabel = new Label(product.getName());
        nameLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #333333; -fx-padding: 0 10 0 10;");
        nameLabel.setWrapText(true);

        Label priceLabel = new Label(String.format("$%.2f", product.getPrice()));
        priceLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #33ccff; -fx-padding: 0 10 5 10;");

        Label descLabel = new Label(product.getDescription());
        descLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #7f8c8d; -fx-padding: 0 10 0 10;");
        descLabel.setWrapText(true);
        descLabel.setMaxHeight(40);

        // Remove button
        Button removeButton = new Button("Remove from Favorites");
        removeButton.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-background-radius: 20;");
        removeButton.setOnAction(e -> {
            favoriteProducts.removeIf(p -> p.getId() == product.getId());
            displayFavorites();
            updateCounter();
        });

        // Button container
        HBox buttonBox = new HBox(10);
        buttonBox.setStyle("-fx-alignment: center; -fx-padding: 5 10 10 10;");
        buttonBox.getChildren().add(removeButton);

        // Add components to card
        card.getChildren().addAll(imageContainer, nameLabel, priceLabel, descLabel, buttonBox);

        return card;
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
            System.err.println("Error loading placeholder image: " + ex.getMessage());
        }
    }

    private void updateCounter() {
        totalItemsLabel.setText(favoriteProducts.size() + " items");
    }

    @FXML
    private void handleBackToProducts() {
        try {
            // Get the correct path to your FXML file
            String fxmlPath = "/com/views/nour/product_client.fxml";
            System.out.println("Attempting to load: " + fxmlPath);

            // Try to get the resource as a stream to verify it exists
            if (getClass().getResourceAsStream(fxmlPath) == null) {
                System.err.println("FXML file not found: " + fxmlPath);
                // You might want to show an alert to the user here
                return;
            }

            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();

            // Get the controller after loading
            ProductFrontController controller = loader.getController();
            // If you need to pass data back to ProductFrontController, do it here

            // Get the current stage and set the new scene
            Stage stage = (Stage) favoritesContainer.getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle("Products");
            stage.show(); // Make sure to show the stage

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Failed to navigate back to products view: " + e.getMessage());
            // Show an alert to the user
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Unexpected error: " + e.getMessage());
        }
    }
}
