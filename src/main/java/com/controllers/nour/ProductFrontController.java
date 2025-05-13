package com.controllers.nour;

import com.exceptions.AuthException;
import com.models.*;
import com.services.*;
import com.services.CommandeService.CartItem;
import com.utils.AuthManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
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
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import com.services.ProductImageService;
import com.services.ProductImageService;

public class ProductFrontController {
    private static final Logger LOGGER = Logger.getLogger(ProductFrontController.class.getName());

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

    // Only need to keep track of favorites locally - cart is in database
    private List<Produit> favoriteProducts = new ArrayList<>();
    private List<Produit> allProducts = new ArrayList<>();

    // Services
    private CommandeService commandeService;
    private FavoriServices favoriService;

    private AuthService authService = new AuthService();

    private User currentUser;

    // Authentication token
    private String token;

    public void setToken(String token) throws AuthException {
        this.token = token;
        if (token != null) {
            currentUser = authService.getUserFromToken(token);
            initialize();
        }
    }

    @FXML
    public void initialize() {
        // Initialize services
        commandeService = new CommandeService();
        favoriService = new FavoriServices();

        // Initialize sorting options
        sortComboBox.setItems(FXCollections.observableArrayList(
                "Newest Arrivals", "Price: Low to High", "Price: High to Low", "Popularity"
        ));
        sortComboBox.setValue("Default");

        // Add listener for sorting
        sortComboBox.setOnAction(event -> applySortingAndFiltering());

        setupSearchListener();
        loadProductsInCardView();

        if (currentUser != null && currentUser.getId() > 0) {
            LOGGER.info("Retrieved user ID from token: " + currentUser.getId());

            // Now we can update counters and load favorites
            updateCounters();
            loadUserFavorites();
        } else {
            LOGGER.warning("No user found from token or user ID is invalid");
        }
    }

    private void setupSearchListener() {
        PauseTransition pause = new PauseTransition(Duration.millis(300));
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            pause.setOnFinished(event -> applySortingAndFiltering());
            pause.playFromStart();
        });
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
            LOGGER.log(Level.SEVERE, "Failed to load products", e);
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

        // Price display with appropriate discount
        boolean isFav = isFavorite(product);
        boolean hasDiscount = false;
        double discountedPrice = product.getPrice();
        String discountType = "";

        // Check for discount eligibility - NEW LOGIC
        DiscountInfo discountInfo = checkDiscountEligibility(product);
        hasDiscount = discountInfo.hasDiscount;
        discountedPrice = discountInfo.discountedPrice;
        discountType = discountInfo.discountType;

        HBox priceBox = new HBox(5);
        priceBox.setAlignment(Pos.CENTER_LEFT);
        priceBox.setStyle("-fx-padding: 0 10 5 10;");

        if (hasDiscount) {
            Label originalPriceLabel = new Label(String.format("%.2f", product.getPrice()));
            originalPriceLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #7f8c8d; -fx-strikethrough: true;");

            Label discountedPriceLabel = new Label(String.format("%.2f", discountedPrice));
            discountedPriceLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #e74c3c; -fx-font-weight: bold;");

            Label discountTypeLabel = new Label(discountType);
            discountTypeLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #2ecc71;");

            VBox priceDetailBox = new VBox(2);
            priceDetailBox.getChildren().addAll(discountedPriceLabel, discountTypeLabel);

            priceBox.getChildren().addAll(originalPriceLabel, priceDetailBox);
        } else {
            Label priceLabel = new Label(String.format("%.2f", product.getPrice()));
            priceLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #33ccff;");
            priceBox.getChildren().add(priceLabel);
        }

        Label descLabel = new Label(product.getDesciption());
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
            applySortingAndFiltering();
        });
        // Add the comment button to your buttonBox
        buttonBox.getChildren().add(addToCartButton);
        favoriteBox.getChildren().add(favoriteButton);
        card.getChildren().addAll(imageContainer, nameLabel, priceBox, descLabel, buttonBox, favoriteBox);

        return card;
    }

    // Helper class to hold discount information
    private static class DiscountInfo {
        boolean hasDiscount;
        double discountedPrice;
        String discountType;

        DiscountInfo(boolean hasDiscount, double discountedPrice, String discountType) {
            this.hasDiscount = hasDiscount;
            this.discountedPrice = discountedPrice;
            this.discountType = discountType;
        }
    }

    // Check discount eligibility with the new logic
    private DiscountInfo checkDiscountEligibility(Produit product) {
        boolean isFav = isFavorite(product);
        boolean hasDiscount = false;
        double discountedPrice = product.getPrice();
        String discountType = "";

        try {
            // Get total products count
            int totalProductsCount = allProducts.size();

            if (totalProductsCount > 0) {
                // Count how many products are in favorites
                int favoriteProductsCount = 0;
                for (Produit p : allProducts) {
                    if (isFavorite(p)) {
                        favoriteProductsCount++;
                    }
                }

                // If half or more of products are in favorites AND this product is NOT in favorites
                if (favoriteProductsCount >= Math.ceil(totalProductsCount / 2.0) && !isFav) {
                    discountedPrice = product.getPrice() * 0.90; // 10% discount
                    discountType = "Special offer!";
                    hasDiscount = true;
                }
            }
            // Keep the existing bulk stock discount as fallback
            else if (!isFav && getProductQuantity(product.getId()) >= 5) {
                discountedPrice = product.getPrice() * 0.9; // 10% discount
                discountType = "Bulk stock discount!";
                hasDiscount = true;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error checking discount status", e);
        }

        return new DiscountInfo(hasDiscount, discountedPrice, discountType);
    }
    // Helper method to get product quantity
    private int getProductQuantity(int productId) throws SQLException {
        ProduitServices produitService = new ProduitServices();
        return produitService.getProductQuantity(productId);
    }

    private void loadProductImage(Produit product, ImageView imageView) {
        ProductImageService.loadProductImage(product, imageView);
    }


    private void loadPlaceholderImage(ImageView imageView) {
        try {
            imageView.setImage(new Image(getClass().getResourceAsStream("/images/placeholder.png")));
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, "Error loading placeholder image: " + ex.getMessage());
        }
    }

    private void addToCart(Produit product) {
        if (currentUser == null || currentUser.getId() <= 0) {
            showAlert("Error", "Please log in to add products to your cart");
            return;
        }

        try {
            // First check if the product is already in the cart
            List<CartItem> cartItems = commandeService.getCartItems(currentUser.getId());
            boolean alreadyInCart = cartItems.stream()
                    .anyMatch(item -> item.getProduit().getId() == product.getId());

            if (alreadyInCart) {
                showAlert("Shopping Cart", "Product '" + product.getName() + "' is already in your cart!");
                return;
            }

            // Get discount information
            DiscountInfo discountInfo = checkDiscountEligibility(product);
            boolean hasDiscount = discountInfo.hasDiscount;
            double discountedPrice = discountInfo.discountedPrice;
            String discountType = discountInfo.discountType;

            LOGGER.info("Adding product to cart - Product ID: " + product.getId() +
                    ", Has Discount: " + hasDiscount +
                    ", Original Price: " + product.getPrice() +
                    ", Discounted Price: " + discountedPrice);

            // Default quantity is 1 when adding to cart for the first time
            if (commandeService.addToCart(currentUser.getId(), product.getId(), 1, hasDiscount, discountedPrice)) {
                String discountMessage = hasDiscount ?
                        "\nYou got a discount: " + discountType + " Price: " + String.format("%.2f", discountedPrice) : "";

                showAlert("Shopping Cart", "Product '" + product.getName() + "' added to cart!" + discountMessage);
                updateCounters();
            } else {
                showAlert("Error", "Failed to add product to cart");
            }
        } catch (SQLException e) {
            showAlert("Error", "Database error: " + e.getMessage());
            LOGGER.log(Level.SEVERE, "Error adding to cart", e);
        }
    }

    private void toggleFavorite(Produit product) {
        if (currentUser == null || currentUser.getId() <= 0) {
            showAlert("Error", "Please log in to add products to your favorites");
            return;
        }

        try {
            if (isFavorite(product)) {
                favoriService.delete(new Favori(currentUser.getId(), product.getId()));
                favoriteProducts.removeIf(p -> p.getId() == product.getId());

                // Calculate favorites info after removal
                int favoriteProductsCount = 0;
                for (Produit p : allProducts) {
                    if (isFavorite(p)) {
                        favoriteProductsCount++;
                    }
                }
                int totalProductsCount = allProducts.size();
                int nonFavoriteCount = totalProductsCount - favoriteProductsCount;

                if (favoriteProductsCount >= Math.ceil(totalProductsCount / 2.0)) {
                    showAlert("Favorites", "Product removed from favorites. Non-favorite products are still eligible for a 10% discount!");
                } else {
                    showAlert("Favorites", "Product removed from favorites. Discounts have been updated.");
                }
            } else {
                favoriService.add(new Favori(currentUser.getId(), product.getId()));
                favoriteProducts.add(product);

                // Calculate favorites info after addition
                int favoriteProductsCount = 0;
                for (Produit p : allProducts) {
                    if (isFavorite(p)) {
                        favoriteProductsCount++;
                    }
                }
                int totalProductsCount = allProducts.size();
                int nonFavoriteCount = totalProductsCount - favoriteProductsCount;

                if (favoriteProductsCount >= Math.ceil(totalProductsCount / 2.0)) {
                    if (nonFavoriteCount > 0) {
                        showAlert("Favorites", "Product added to favorites. All non-favorite products are now eligible for a 10% discount!");
                    } else {
                        showAlert("Favorites", "Product added to favorites. All products are now favorites!");
                    }
                } else {
                    showAlert("Favorites", "Product added to favorites.");
                }
            }
            updateCounters();

            // Refresh the display to update prices after modifying favorites
            applySortingAndFiltering();
        } catch (SQLException e) {
            showAlert("Error", "Failed to update favorites: " + e.getMessage());
            LOGGER.log(Level.SEVERE, "Error toggling favorite", e);
        }
    }
    private boolean isFavorite(Produit product) {
        if (currentUser == null || currentUser.getId() <= 0) {
            return false;
        }

        try {
            return favoriService.isProductInFavorites(currentUser.getId(), product.getId());
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error checking favorite status", e);
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
            LOGGER.log(Level.SEVERE, "Failed to load favorites statistics view", e);
            showAlert("Error", "Failed to load favorites statistics view: " + e.getMessage());
        }
    }

    private void updateCounters() {
        if (currentUser == null || currentUser.getId() <= 0) {
            cartCountLabel.setText("0");
            favoritesCountLabel.setText("0");
            return;
        }

        try {
            // Get cart count from database
            List<CartItem> cartItems = commandeService.getCartItems(currentUser.getId());
            cartCountLabel.setText(String.valueOf(cartItems.size()));

            // Get favorites count
            favoritesCountLabel.setText(String.valueOf(favoriteProducts.size()));
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to update counters", e);
            showAlert("Error", "Failed to update counters: " + e.getMessage());
        }
    }

    @FXML
    private void handleViewCart() {
        if (currentUser == null || currentUser.getId() <= 0) {
            showAlert("Error", "Please log in to view your cart");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/views/nour/cart_view.fxml"));
            Parent root = loader.load();

            CartViewController controller = loader.getController();
            controller.setToken(this.token);

            // Create and set the scene
            Stage stage = (Stage) productContainer.getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle("Shopping Cart");

            // Check for any pending navigation
            controller.checkPendingNavigation();
        } catch (IOException | AuthException e) {
            LOGGER.log(Level.SEVERE, "Failed to navigate to cart view", e);
            showAlert("Error", "Failed to navigate to cart view: " + e.getMessage());
        }
    }

    @FXML
    private void handleViewFavorites() {
        if (currentUser == null || currentUser.getId() <= 0) {
            showAlert("Error", "Please log in to view your favorites");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/views/nour/favorites_view.fxml"));
            Parent root = loader.load();

            FavoritesViewController controller = loader.getController();
            controller.setFavoriteProducts(favoriteProducts);
            controller.setToken(this.token);

            Stage stage = (Stage) productContainer.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Favorites");
        } catch (IOException | AuthException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to navigate to favorites view: " + e.getMessage());
        }
    }

    @FXML
    private void handleBackToAdmin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/views/nour/admin_dashboard.fxml"));
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
        if (currentUser == null || currentUser.getId() <= 0) {
            favoriteProducts.clear();
            return;
        }

        try {
            favoriteProducts = favoriService.getFavoriteProducts(currentUser.getId());
            updateCounters();
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to load favorites: " + e.getMessage());
        }
    }

    // This method is kept for backward compatibility
    public void setCartProducts(List<Produit> cartProducts) {
        // No longer needed as we're using the database
        updateCounters();
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
                                    product.getDesciption().toLowerCase().contains(searchText)
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

        }
        displayProducts(filteredProducts);
    }
}