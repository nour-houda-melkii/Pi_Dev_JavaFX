package com.controllers.nour;

import com.models.*;
import com.services.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ComboBox;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.animation.PauseTransition;
import javafx.collections.FXCollections;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;


public class ProductFrontController {
    @FXML
    private FlowPane productContainer;

    @FXML
    private TextField searchField;

    @FXML
    private ComboBox<String> sortComboBox;

    @FXML
    private Label cartCountLabel;

    @FXML
    private Label favoritesCountLabel;

    @FXML
    private ImageView logoImageView;

    // Lists to keep track of cart and favorites
    private List<Produit> cartProducts = new ArrayList<>();
    private List<Produit> favoriteProducts = new ArrayList<>();
    private List<Produit> allProducts = new ArrayList<>();

    @FXML
    public void initialize() {
        // Initialize sorting options
        sortComboBox.setItems(FXCollections.observableArrayList(
                "Default",
                "Name (A-Z)",
                "Name (Z-A)",
                "Price (Low-High)",
                "Price (High-low)"
        ));
        sortComboBox.setValue("Default");

        // Add listener for sorting
        sortComboBox.setOnAction(event -> applySortingAndFiltering());

        setupSearchListener();
        loadProductsInCardView();
        updateCounters();
        loadUserFavorites();
    }

    private void setupSearchListener() {
        PauseTransition pause = new PauseTransition(Duration.millis(300));
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            pause.setOnFinished(event -> applySortingAndFiltering());
            pause.playFromStart();
        });
    }

    private void applySortingAndFiltering() {
        String searchText = searchField.getText().toLowerCase();
        List<Produit> filteredProducts;

        if (searchText == null || searchText.isEmpty()) {
            filteredProducts = new ArrayList<>(allProducts);
        } else {
            filteredProducts = allProducts.stream()
                    .filter(product ->
                            product.getName().toLowerCase().contains(searchText) ||
                                    product.getDescription().toLowerCase().contains(searchText)
                    )
                    .collect(Collectors.toList());
        }

        String sortOption = sortComboBox.getValue();
        switch (sortOption) {
            case "Name (A-Z)":
                filteredProducts.sort(Comparator.comparing(Produit::getName));
                break;
            case "Name (Z-A)":
                filteredProducts.sort(Comparator.comparing(Produit::getName).reversed());
                break;
            case "Price (Low-High)":
                filteredProducts.sort(Comparator.comparing(Produit::getPrice));
                break;
            case "Price (High-Low)":
                filteredProducts.sort(Comparator.comparing(Produit::getPrice).reversed());
                break;
            default:
                filteredProducts.sort(Comparator.comparing(Produit::getId));
                break;
        }

        displayProducts(filteredProducts);
    }

    private void displayProducts(List<Produit> products) {
        productContainer.getChildren().clear();
        for (Produit product : products) {
            productContainer.getChildren().add(createProductCard(product));
        }
    }

    private void loadProductsInCardView() {
        try {
            ProduitServices produitService = new ProduitServices();
            allProducts = produitService.showAll();
            applySortingAndFiltering();
        } catch (SQLException e) {
            showAlert("Error", "Failed to load products: " + e.getMessage());
        }
    }

    private VBox createProductCard(Produit product) {
        // Create a styled card container
        VBox card = new VBox(10);
        card.setPrefWidth(200);
        card.setPrefHeight(350);
        card.setStyle("-fx-background-color: white; " +
                "-fx-border-color: black; " +
                "-fx-border-width: 1.5; " +
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

        // Price display with discount if favorite
        boolean isFav = isFavorite(product);
        double discountedPrice = product.getPrice() * 0.95;

        HBox priceBox = new HBox(5);
        priceBox.setAlignment(Pos.CENTER_LEFT);
        priceBox.setStyle("-fx-padding: 0 10 5 10;");

        if (isFav) {
            Label originalPriceLabel = new Label(String.format("$%.2f", product.getPrice()));
            originalPriceLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #7f8c8d; -fx-strikethrough: true;");

            Label discountedPriceLabel = new Label(String.format("$%.2f", discountedPrice));
            discountedPriceLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #e74c3c; -fx-font-weight: bold;");

            priceBox.getChildren().addAll(originalPriceLabel, discountedPriceLabel);
        } else {
            Label priceLabel = new Label(String.format("$%.2f", product.getPrice()));
            priceLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #33ccff;");
            priceBox.getChildren().add(priceLabel);
        }

        Label descLabel = new Label(product.getDescription());
        descLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #7f8c8d; -fx-padding: 0 10 0 10;");
        descLabel.setWrapText(true);
        descLabel.setMaxHeight(40);

        // Button container
        HBox buttonBox = new HBox(10);
        buttonBox.setStyle("-fx-alignment: center; -fx-padding: 5 10 5 10;");

        // Add to Cart Button
        Button addToCartButton = new Button("Add to Cart");
        addToCartButton.setStyle("-fx-background-color: #33ccff; -fx-text-fill: white; -fx-background-radius: 20;");
        addToCartButton.setOnAction(e -> {
            addToCart(product);
            showAlert("Shopping Cart", "Product '" + product.getName() + "' added to cart!");
        });

        // Favorite Button container
        HBox favoriteBox = new HBox(10);
        favoriteBox.setStyle("-fx-alignment: center; -fx-padding: 0 10 10 10;");

        // Favorite Button
        Button favoriteButton = new Button("♥");
        favoriteButton.setStyle("-fx-background-color: " + (isFav ? "#ff3366" : "#f0f0f0") +
                "; -fx-text-fill: white; -fx-background-radius: 20; -fx-min-width: 40px; -fx-min-height: 40px; -fx-font-size: 18px;");
        favoriteButton.setOnAction(e -> {
            toggleFavorite(product);
            // Refresh the card to show updated price
            displayProducts(allProducts);
        });

        // Add components to card
        buttonBox.getChildren().add(addToCartButton);
        favoriteBox.getChildren().add(favoriteButton);
        card.getChildren().addAll(imageContainer, nameLabel, priceBox, descLabel, buttonBox, favoriteBox);

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
            System.err.println("Error loading placeholder image: " + ex.getMessage());
        }
    }

    private void addToCart(Produit product) {
        cartProducts.add(product);
        updateCounters();
    }

    private void toggleFavorite(Produit product) {
        try {
            FavoriServices favoriService = new FavoriServices();
            int currentUserId = 1; // Replace with actual user ID

            if (isFavorite(product)) {
                favoriService.delete(new Favori(currentUserId, product.getId()));
                favoriteProducts.removeIf(p -> p.getId() == product.getId());
            } else {
                favoriService.add(new Favori(currentUserId, product.getId()));
                favoriteProducts.add(product);
            }
            updateCounters();
        } catch (SQLException e) {
            showAlert("Error", "Failed to update favorites: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private boolean isFavorite(Produit product) {
        try {
            int currentUserId = 1; // Replace with actual user ID
            FavoriServices favoriService = new FavoriServices();
            return favoriService.isProductInFavorites(currentUserId, product.getId());
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    @FXML
    private void handleShowFavoritesStatistics() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/views/nour/favorites_statistics_view.fxml"));
            Parent root = loader.load();

            Stage statisticsStage = new Stage();
            statisticsStage.setScene(new Scene(root));
            statisticsStage.setTitle("Favorites Statistics");
            statisticsStage.initModality(Modality.APPLICATION_MODAL);
            statisticsStage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to load favorites statistics view: " + e.getMessage());
        }
    }

    private void updateCounters() {
        cartCountLabel.setText(String.valueOf(cartProducts.size()));
        favoritesCountLabel.setText(String.valueOf(favoriteProducts.size()));
    }

    @FXML
    private void handleViewCart() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/views/nour/cart_view.fxml"));
            Parent root = loader.load();

            CartViewController controller = loader.getController();
            controller.setCartProducts(cartProducts);

            Stage stage = (Stage) productContainer.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Shopping Cart");
        } catch (IOException e) {
            showAlert("Error", "Failed to navigate to cart view: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleViewFavorites() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/views/nour/favorites_view.fxml"));
            Parent root = loader.load();

            FavoritesViewController controller = loader.getController();
            controller.setFavoriteProducts(favoriteProducts);

            Stage stage = (Stage) productContainer.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Favorites");
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to navigate to favorites view: " + e.getMessage());
        }
    }

    @FXML
    private void handleBackToAdmin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/views/AdminDashboard.fxml"));
            Stage stage = (Stage) productContainer.getScene().getWindow();
            stage.setScene(new Scene(loader.load()));
            stage.setTitle("Admin Dashboard");
        } catch (IOException e) {
            showAlert("Error", "Failed to return to admin dashboard");
        }
    }
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void loadUserFavorites() {
        try {
            int currentUserId = 1; // Replace with actual user ID
            FavoriServices favoriService = new FavoriServices();
            favoriteProducts = favoriService.getFavoriteProducts(currentUserId);
            updateCounters();
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to load favorites: " + e.getMessage());
        }
    }

    public void setCartProducts(List<Produit> cartProducts) {
    }
}