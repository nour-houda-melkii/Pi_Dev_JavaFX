package controllers;

import entities.Produit;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
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
        Label descLabel = new Label(product.getDescription());
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
            favoriteProducts.removeIf(p -> p.getId() == product.getId());
            displayFavorites();
            updateCounter();
        });

        // Add buttons to container
        buttonBox.getChildren().addAll(removeButton);

        // Add all components to card
        card.getChildren().addAll(imageContainer, detailsContainer, buttonBox);

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
        totalItemsLabel.setText(favoriteProducts.size() + " favorite items");
        totalItemsLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #0056b3;");
    }

    @FXML
    private void handleBackToProducts() {
        try {
            // Get the correct path to your FXML file
            String fxmlPath = "/product_client.fxml";
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