package com.controllers.nour;

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
    private CommentaireService commentaireService;
    private FavoriServices favoriService;

    // Current user ID - will be set via the setter method
    private int currentUserId;

    @FXML
    public void initialize() {
        // Initialize services
        commandeService = new CommandeService();
        favoriService = new FavoriServices();
        commentaireService = new CommentaireService();

        // Initialize sorting options
        sortComboBox.setItems(FXCollections.observableArrayList(
                "Default",
                "Name (A-Z)",
                "Name (Z-A)",
                "Price (Low-High)",
                "Price (High-low)",
                "Most Comments"  // New option

        ));
        sortComboBox.setValue("Default");

        // Add listener for sorting
        sortComboBox.setOnAction(event -> applySortingAndFiltering());

        setupSearchListener();
        loadProductsInCardView();

        // Check if there's a stored user ID in AuthManager
        int storedUserId = AuthManager.getUserId();
        if (storedUserId > 0) {
            // Set the user ID from AuthManager
            this.currentUserId = storedUserId;
            LOGGER.info("Retrieved user ID from AuthManager: " + currentUserId);

            // Now we can update counters and load favorites
            updateCounters();
            loadUserFavorites();
        } else {
            LOGGER.warning("No user ID found in AuthManager");
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
        Button commentButton = new Button("Comments");
        commentButton.setStyle("-fx-background-color: #33ccff; -fx-text-fill: white; -fx-background-radius: 20;");
        commentButton.setOnAction(e -> showCommentsDialog(product));

        // Add the comment button to your buttonBox
        buttonBox.getChildren().add(addToCartButton);
        favoriteBox.getChildren().add(favoriteButton);
        card.getChildren().addAll(imageContainer, nameLabel, priceBox, descLabel, buttonBox, favoriteBox);

        VBox commentsSection = new VBox(5);
        commentsSection.setStyle("-fx-padding: 0 10 10 10;");

        Label commentsTitle = new Label("Comments:");
        commentsTitle.setStyle("-fx-font-weight: bold;");
        commentsSection.getChildren().add(commentsTitle);

        try {
            List<Commentaire> comments = commentaireService.getCommentsForProduct(product.getId());
            if (comments.isEmpty()) {
                Label noComments = new Label("No comments yet");
                noComments.setStyle("-fx-font-style: italic; -fx-text-fill: gray;");
                commentsSection.getChildren().add(noComments);
            } else {
                // Limit to showing 2-3 comments with a "Show more" option
                int maxCommentsToShow = 2;
                for (int i = 0; i < Math.min(maxCommentsToShow, comments.size()); i++) {
                    commentsSection.getChildren().add(createCommentNode(comments.get(i)));
                }

                if (comments.size() > maxCommentsToShow) {
                    Button showMoreBtn = new Button("Show more...");
                    showMoreBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #33ccff;");
                    showMoreBtn.setOnAction(e -> showAllComments(product));
                    commentsSection.getChildren().add(showMoreBtn);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error loading comments", e);
            Label errorLabel = new Label("Error loading comments");
            errorLabel.setStyle("-fx-text-fill: red;");
            commentsSection.getChildren().add(errorLabel);
        }

        // Add comment input field
        if (currentUserId > 0) {
            HBox addCommentBox = new HBox(5);
            TextField commentField = new TextField();
            commentField.setPromptText("Add a comment...");
            Button postButton = new Button("Post");
            postButton.setStyle("-fx-background-color: #33ccff; -fx-text-fill: white;");
            postButton.setOnAction(e -> {
                addComment(product, commentField.getText());
                commentField.clear();
            });

            addCommentBox.getChildren().addAll(commentField, postButton);
            commentsSection.getChildren().add(addCommentBox);
        }

        card.getChildren().add(commentsSection);

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
            LOGGER.log(Level.WARNING, "Error loading placeholder image: " + ex.getMessage());
        }
    }

    private void addToCart(Produit product) {
        if (currentUserId <= 0) {
            showAlert("Error", "Please log in to add products to your cart");
            return;
        }

        try {
            // First check if the product is already in the cart
            List<CartItem> cartItems = commandeService.getCartItems(currentUserId);
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

            // Default quantity is 1 when adding to cart for the first time
            if (commandeService.addToCart(currentUserId, product.getId(), 1, hasDiscount, discountedPrice)) {
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
        if (currentUserId <= 0) {
            showAlert("Error", "Please log in to add products to your favorites");
            return;
        }

        try {
            if (isFavorite(product)) {
                favoriService.delete(new Favori(currentUserId, product.getId()));
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
                favoriService.add(new Favori(currentUserId, product.getId()));
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
        if (currentUserId == 0) {
            return false;
        }

        try {
            return favoriService.isProductInFavorites(currentUserId, product.getId());
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
        if (currentUserId <= 0) {
            cartCountLabel.setText("0");
            favoritesCountLabel.setText("0");
            return;
        }

        try {
            // Get cart count from database
            List<CartItem> cartItems = commandeService.getCartItems(currentUserId);
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
        if (currentUserId <= 0) {
            showAlert("Error", "Please log in to view your cart");
            return;
        }

        try {
            // First, ensure the user ID is stored in AuthManager for backup access
            AuthManager.storeUserId(currentUserId);

            // Log auth state before navigation
            AuthManager.logAuthState();

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/views/nour/cart_view.fxml"));
            Parent root = loader.load();

            CartViewController controller = loader.getController();

            // Make sure user ID is valid before setting it
            if (currentUserId > 0) {
                controller.setCurrentUserId(currentUserId);

                // Create and set the scene FIRST before checking pending navigation
                Stage stage = (Stage) productContainer.getScene().getWindow();
                Scene scene = new Scene(root);
                stage.setScene(scene);
                stage.setTitle("Shopping Cart");

                // AFTER the scene is set, check for any pending navigation
                controller.checkPendingNavigation();
            } else {
                showAlert("Error", "Invalid user ID. Please log in again.");
                return;
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to navigate to cart view", e);
            showAlert("Error", "Failed to navigate to cart view: " + e.getMessage());
        }
    }

    @FXML
    private void handleViewFavorites() {
        try {
            // First, ensure the user ID is stored in AuthManager for backup access
            if (currentUserId > 0) {
                AuthManager.storeUserId(currentUserId);
            }

            // Log auth state before navigation
            AuthManager.logAuthState();

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/views/nour/favorites_view.fxml"));
            Parent root = loader.load();

            FavoritesViewController controller = loader.getController();
            controller.setFavoriteProducts(favoriteProducts);

            // Pass the current user ID to the favorites controller
            controller.setCurrentUserId(currentUserId);

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
        try {
            favoriteProducts = favoriService.getFavoriteProducts(currentUserId);
            updateCounters();
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to load favorites: " + e.getMessage());
        }
    }

    // Method to update the current user ID (e.g., after login)
    public void setCurrentUserId(int userId) {
        this.currentUserId = userId;
        loadUserFavorites();
        updateCounters();
        loadProductsInCardView(); // Reload products to show updated favorite states
    }

    // This method is kept for backward compatibility
    public void setCartProducts(List<Produit> cartProducts) {
        // No longer needed as we're using the database
        updateCounters();
    }
    private void showCommentsDialog(Produit product) {
        try {
            // Load the comments dialog FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/views/nour/comments_dialog.fxml"));

            // Set the controller factory to ensure proper initialization
            loader.setControllerFactory(clazz -> {
                try {
                    CommentsDialogController controller = new CommentsDialogController();
                    controller.setProduct(product);
                    controller.setCurrentUserId(currentUserId);
                    return controller;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

            Parent root = loader.load();

            // Get the controller after loading
            CommentsDialogController controller = loader.getController();
            controller.loadComments();

            // Create and show the dialog
            Stage dialogStage = new Stage();
            dialogStage.setTitle("Comments for " + product.getName());
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.setScene(new Scene(root));
            dialogStage.showAndWait();

            // After dialog closes, refresh products to update sorting by comments
            loadProductsInCardView();
        } catch (IOException e) {
            showAlert("Error", "Failed to load comments dialog: " + e.getMessage());
            LOGGER.log(Level.SEVERE, "Error loading comments dialog", e);
        }
    }
    // Update your applySortingAndFiltering method to include comment sorting
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
            case "Most Comments":
                try {
                    // Get comment counts for all products
                    List<CommentaireService.ProductCommentCount> commentCounts = commentaireService.getProductCommentCounts();

                    // Create a map of product ID to comment count
                    Map<Integer, Integer> commentCountMap = commentCounts.stream()
                            .collect(Collectors.toMap(
                                    CommentaireService.ProductCommentCount::getProduitId,
                                    CommentaireService.ProductCommentCount::getCommentCount));

                    // Sort by comment count (descending)
                    filteredProducts.sort((p1, p2) -> {
                        int count1 = commentCountMap.getOrDefault(p1.getId(), 0);
                        int count2 = commentCountMap.getOrDefault(p2.getId(), 0);
                        return Integer.compare(count2, count1);
                    });
                } catch (SQLException e) {
                    LOGGER.log(Level.SEVERE, "Error sorting by comments", e);
                    // Fall back to default sorting
                    filteredProducts.sort(Comparator.comparing(Produit::getId));
                }
                break;
            default:
                filteredProducts.sort(Comparator.comparing(Produit::getId));
                break;
        }

        displayProducts(filteredProducts);
    }
    private void deleteComment(Commentaire comment) {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Delete Comment");
        confirmation.setHeaderText("Are you sure you want to delete this comment?");
        confirmation.setContentText("This action cannot be undone.");

        confirmation.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    commentaireService.deleteComment(comment.getId(), currentUserId);
                    // Refresh the product card
                    loadProductsInCardView();
                } catch (SQLException e) {
                    showAlert("Error", "Failed to delete comment: " + e.getMessage());
                    LOGGER.log(Level.SEVERE, "Error deleting comment", e);
                }
            }
        });
    }

    private void showAllComments(Produit product) {
        try {
            List<Commentaire> comments = commentaireService.getCommentsForProduct(product.getId());

            Stage dialog = new Stage();
            VBox dialogContent = new VBox(10);
            dialogContent.setStyle("-fx-padding: 15;");

            ScrollPane scrollPane = new ScrollPane();
            VBox commentsBox = new VBox(5);

            for (Commentaire comment : comments) {
                commentsBox.getChildren().add(createCommentNode(comment));
            }

            scrollPane.setContent(commentsBox);
            scrollPane.setFitToWidth(true);

            if (currentUserId > 0) {
                HBox addCommentBox = new HBox(5);
                TextField commentField = new TextField();
                commentField.setPromptText("Add a comment...");
                Button postButton = new Button("Post");
                postButton.setStyle("-fx-background-color: #33ccff; -fx-text-fill: white;");
                postButton.setOnAction(e -> {
                    addComment(product, commentField.getText());
                    commentField.clear();
                    dialog.close();
                    showAllComments(product); // Reopen to show updated comments
                });

                addCommentBox.getChildren().addAll(commentField, postButton);
                dialogContent.getChildren().addAll(scrollPane, addCommentBox);
            } else {
                dialogContent.getChildren().add(scrollPane);
            }

            dialog.setScene(new Scene(dialogContent, 300, 400));
            dialog.setTitle("All Comments for " + product.getName());
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.show();
        } catch (SQLException e) {
            showAlert("Error", "Failed to load comments: " + e.getMessage());
            LOGGER.log(Level.SEVERE, "Error loading comments", e);
        }
    }
    private Node createCommentNode(Commentaire comment) {
        VBox commentBox = new VBox(3);
        commentBox.setStyle("-fx-padding: 5; -fx-background-color: #f5f5f5; -fx-background-radius: 5;");

        // Comment header with user ID and date
        Label headerLabel = new Label("User #" + comment.getUserId() + " - " +
                comment.getCreatedAt().toString());
        headerLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #666;");

        // Comment content
        Label contentLabel = new Label(comment.getContent());
        contentLabel.setStyle("-fx-font-size: 12px;");
        contentLabel.setWrapText(true);

        commentBox.getChildren().addAll(headerLabel, contentLabel);

        // Add delete button if this is the current user's comment
        if (comment.getUserId() == currentUserId) {
            Button deleteButton = new Button("Delete");
            deleteButton.setStyle("-fx-background-color: #ff3333; -fx-text-fill: white; -fx-font-size: 10px;");
            deleteButton.setOnAction(e -> deleteComment(comment));
            commentBox.getChildren().add(deleteButton);
        }

        return commentBox;
    }

    private void addComment(Produit product, String content) {
        if (content == null || content.trim().isEmpty()) {
            showAlert("Error", "Comment cannot be empty");
            return;
        }

        try {
            Commentaire comment = new Commentaire(product.getId(), currentUserId, content.trim());
            commentaireService.addComment(comment);
            // Refresh the product card to show the new comment
            loadProductsInCardView();
        } catch (SQLException e) {
            showAlert("Error", "Failed to add comment: " + e.getMessage());
            LOGGER.log(Level.SEVERE, "Error adding comment", e);
        }
    }
}