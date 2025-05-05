package com.controllers.nour;

import com.models.Commande;
import com.models.Produit;
import com.models.User;
import com.services.CommandeService;
import com.services.CommandeService.CartItem;
import com.services.FavoriServices;
import com.services.UserService;
import com.services.AuthService;
import com.utils.AuthManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class CartViewController {
    private static final Logger LOGGER = Logger.getLogger(CartViewController.class.getName());

    @FXML
    private VBox cartItemsContainer;

    @FXML
    private Label totalLabel;

    @FXML
    private Button checkoutButton;

    @FXML
    private Button continueShoppingButton;

    @FXML
    private Label emptyCartLabel;

    private CommandeService commandeService;
    private UserService userService;
    private int currentUserId;
    private User loggedInUser;
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.US);
    private List<CartItem> currentCartItems;
    private boolean needsNavigationToLogin = false;

    @FXML
    public void initialize() {
        commandeService = new CommandeService();
        userService = new UserService();

        // Add debug logging to see what's happening
        AuthManager.logAuthState();

        // Check if user is logged in via AuthManager
        currentUserId = AuthManager.getUserId();

        LOGGER.info("Cart initialized with user ID from AuthManager: " + currentUserId);

        if (currentUserId <= 0) {
            LOGGER.warning("No valid user ID from AuthManager. User needs to log in.");
            showEmptyCartMessage();
            showAlert("Authentication Required", "Please log in to view your cart.");
            // Mark for navigation but don't navigate yet - scene may not be ready
            needsNavigationToLogin = true;
        } else {
            // User ID is valid, load user data
            loadUserData();
        }
    }

    private void loadUserData() {
        try {
            // Attempt to load user data
            this.loggedInUser = userService.getUserById(currentUserId);

            // MODIFIED: This is the key fix - we can proceed with cart functionality even if user object is null
            // If we have a valid user ID, we should be able to load cart items even if detailed user data isn't available
            if (loggedInUser != null) {
                LOGGER.info("Cart initialized for user: " + loggedInUser.getEmail());
            } else {
                LOGGER.warning("User data not found for ID: " + currentUserId + ", but continuing with cart loading");
            }

            // Always proceed to load cart items if we have a valid user ID
            loadCartItems();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to load user data", e);
            // Still try to load cart items despite user data loading failure
            loadCartItems();
        }
    }

    /**
     * This method is called after the scene is fully set up to perform any pending navigation
     */
    public void checkPendingNavigation() {
        if (needsNavigationToLogin) {
            navigateToLogin();
            needsNavigationToLogin = false;
        }
    }

    public void setCurrentUserId(int userId) {
        LOGGER.info("Setting current user ID to: " + userId);

        if (userId <= 0) {
            LOGGER.warning("Invalid user ID provided: " + userId);
            showAlert("Authentication Required", "Please log in to view your cart.");
            showEmptyCartMessage();
            // Mark for navigation but don't navigate yet
            needsNavigationToLogin = true;
            return;
        }

        this.currentUserId = userId;

        // Make sure the ID is stored in AuthManager
        AuthManager.storeUserId(userId);

        try {
            // Try to load user data but don't block cart functionality if it fails
            this.loggedInUser = userService.getUserById(userId);

            if (loggedInUser != null) {
                LOGGER.info("Successfully loaded user data for ID: " + userId);
            } else {
                LOGGER.warning("User data not found for ID: " + userId + ", but continuing with cart loading");
            }

            // Always proceed to load cart items if we have a valid user ID
            loadCartItems();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to load user data for ID: " + userId, e);
            // Continue with cart functionality despite user data loading failure
            loadCartItems();
        }
    }

    private void loadCartItems() {
        try {
            LOGGER.info("Loading cart items for user ID: " + currentUserId);

            currentCartItems = commandeService.getCartItems(currentUserId);
            cartItemsContainer.getChildren().clear();

            if (currentCartItems == null || currentCartItems.isEmpty()) {
                LOGGER.info("No items in cart for user ID: " + currentUserId);
                showEmptyCartMessage();
                checkoutButton.setDisable(true);
                totalLabel.setText("Total: $0.00");
            } else {
                LOGGER.info("Found " + currentCartItems.size() + " items in cart");
                emptyCartLabel.setVisible(false);
                checkoutButton.setDisable(false);
                displayCartItems(currentCartItems);
                calculateAndDisplayTotal(currentCartItems);
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error while loading cart items", e);
            showAlert("Error", "Failed to load cart items: " + e.getMessage());
            showEmptyCartMessage();
        }
    }

    private void showEmptyCartMessage() {
        if (emptyCartLabel != null) {
            emptyCartLabel.setVisible(true);
            emptyCartLabel.setText("Your cart is empty.");
            emptyCartLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #7f8c8d;");
        }

        if (checkoutButton != null) {
            checkoutButton.setDisable(true);
        }

        if (totalLabel != null) {
            totalLabel.setText("Total: $0.00");
        }

        if (cartItemsContainer != null) {
            cartItemsContainer.getChildren().clear();
        }
    }

    private void displayCartItems(List<CartItem> cartItems) throws SQLException {
        for (CartItem item : cartItems) {
            cartItemsContainer.getChildren().add(createCartItemRow(item));
        }
    }

    private HBox createCartItemRow(CartItem item) throws SQLException {
        // Main container
        HBox itemRow = new HBox(15);
        itemRow.setAlignment(Pos.CENTER_LEFT);
        itemRow.setPrefHeight(100);
        itemRow.setStyle("-fx-padding: 10; -fx-border-color: #e0e0e0; -fx-border-width: 0 0 1 0;");

        // Product Image
        ImageView imageView = new ImageView();
        imageView.setFitWidth(80);
        imageView.setFitHeight(80);
        imageView.setPreserveRatio(true);

        try {
            if (item.getProduit().getImagePath() != null && !item.getProduit().getImagePath().isEmpty()) {
                File file = new File(item.getProduit().getImagePath());
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

        // Product Details VBox
        VBox detailsBox = new VBox(5);
        detailsBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(detailsBox, Priority.ALWAYS);

        Label nameLabel = new Label(item.getProduit().getName());
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        // Get discount information using our new methods
        FavoriServices favoriService = new FavoriServices();
        double originalPrice = item.getProduit().getPrice();
        double finalPrice = originalPrice;
        String discountType = "";

        try {
            // Check if product is eligible for "unpopular items" discount
            if (favoriService.isProductEligibleForDiscount(item.getProduit().getId())) {
                finalPrice = originalPrice * 0.90; // 10% discount
                discountType = "Special promotion: 10% off";
            }
            // If not eligible for "unpopular" discount but is in favorites
            else if (favoriService.isProductInFavorites(currentUserId, item.getProduit().getId())) {
                finalPrice = originalPrice * 0.95; // 5% discount
                discountType = "Favorite discount: 5% off";
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error checking discount status", e);
        }

        // Create price display
        VBox priceDetailsBox = new VBox(3);
        priceDetailsBox.setAlignment(Pos.CENTER_LEFT);

        // Show original price (with strikethrough if discounted)
        if (finalPrice < originalPrice) {
            // Original price with strikethrough
            Label originalPriceLabel = new Label("Original price: " + currencyFormat.format(originalPrice));
            originalPriceLabel.setStyle("-fx-text-fill: #7f8c8d; -fx-strikethrough: true;");

            // Final price with discount type
            Label finalPriceLabel = new Label(currencyFormat.format(finalPrice));
            finalPriceLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");

            // Discount type label
            Label discountLabel = new Label(discountType);
            discountLabel.setStyle("-fx-text-fill: #2ecc71; -fx-font-size: 10px;");

            priceDetailsBox.getChildren().addAll(originalPriceLabel, finalPriceLabel, discountLabel);
        } else {
            // Regular price without discount
            Label priceLabel = new Label(currencyFormat.format(originalPrice));
            priceLabel.setStyle("-fx-text-fill: #33ccff; -fx-font-weight: bold;");
            priceDetailsBox.getChildren().add(priceLabel);
        }

        // Quantity controls
        HBox quantityBox = new HBox(5);
        quantityBox.setAlignment(Pos.CENTER);

        Button decreaseButton = new Button("-");
        decreaseButton.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;");
        decreaseButton.setOnAction(e -> updateItemQuantity(item, item.getQuantity() - 1));

        Label quantityLabel = new Label(String.valueOf(item.getQuantity()));
        quantityLabel.setStyle("-fx-font-weight: bold; -fx-min-width: 30px; -fx-alignment: center;");

        Button increaseButton = new Button("+");
        increaseButton.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white;");
        increaseButton.setOnAction(e -> updateItemQuantity(item, item.getQuantity() + 1));

        quantityBox.getChildren().addAll(decreaseButton, quantityLabel, increaseButton);

        // Remove button
        Button removeButton = new Button("Remove");
        removeButton.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;");
        removeButton.setOnAction(e -> removeItemFromCart(item));

        // Subtotal - use the final price with discount applied
        double subtotal = finalPrice * item.getQuantity();
        Label subtotalLabel = new Label(currencyFormat.format(subtotal));
        subtotalLabel.setStyle("-fx-font-weight: bold; -fx-min-width: 80px;");

        // Add all components to the row
        detailsBox.getChildren().addAll(nameLabel, priceDetailsBox, quantityBox);
        itemRow.getChildren().addAll(imageView, detailsBox, subtotalLabel, removeButton);

        return itemRow;
    }
    private void loadPlaceholderImage(ImageView imageView) {
        try {
            imageView.setImage(new Image(getClass().getResourceAsStream("/images/placeholder.png")));
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, "Error loading placeholder image", ex);
        }
    }

    private void updateItemQuantity(CartItem item, int newQuantity) {
        try {
            if (commandeService.updateCartItemQuantity(item.getId(), newQuantity)) {
                loadCartItems(); // Refresh the cart view
            } else {
                showAlert("Error", "Failed to update item quantity.");
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error while updating quantity", e);
            showAlert("Error", "Database error: " + e.getMessage());
        }
    }

    private void removeItemFromCart(CartItem item) {
        try {
            if (commandeService.removeFromCart(currentUserId, item.getProduit().getId())) {
                loadCartItems(); // Refresh the cart view
            } else {
                showAlert("Error", "Failed to remove item from cart.");
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error while removing item", e);
            showAlert("Error", "Database error: " + e.getMessage());
        }
    }

    private void calculateAndDisplayTotal(List<CartItem> cartItems) {
        double total = 0;
        double originalTotal = 0;
        double savedAmount = 0;
        FavoriServices favoriService = new FavoriServices();

        for (CartItem item : cartItems) {
            try {
                double originalPrice = item.getProduit().getPrice();
                double finalPrice = originalPrice;
                boolean isFav = favoriService.isProductInFavorites(currentUserId, item.getProduit().getId());
                int favoriteCount = favoriService.getFavoriteCount(item.getProduit().getId());

                // Check for special promotion discount (10% off for products with no favorites when others have 2+ favorites)
                if (favoriteCount >= 2) {
                    List<Produit> noFavProducts = favoriService.getProductsWithNoFavorites();
                    for (Produit noFavProduct : noFavProducts) {
                        if (noFavProduct.getId() == item.getProduit().getId()) {
                            finalPrice = originalPrice * 0.90; // 10% discount
                            break;
                        }
                    }
                }

                // Check for favorite discount (5% off for favorite products)
                else if (isFav) {
                    finalPrice = originalPrice * 0.95; // 5% discount
                }

                // Calculate totals
                originalTotal += originalPrice * item.getQuantity();
                total += finalPrice * item.getQuantity();

            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Error calculating discounts for total", e);
                // If there's an error, use the original price
                total += item.getProduit().getPrice() * item.getQuantity();
                originalTotal += item.getProduit().getPrice() * item.getQuantity();
            }
        }

        // Calculate savings
        savedAmount = originalTotal - total;

        // Create a VBox to display total information
        VBox totalInfoBox = new VBox(5);
        totalInfoBox.setAlignment(Pos.CENTER_RIGHT);

        // Only show savings information if there are discounts
        if (savedAmount > 0) {
            Label originalTotalLabel = new Label("Original Total: " + currencyFormat.format(originalTotal));
            originalTotalLabel.setStyle("-fx-text-fill: #7f8c8d; -fx-strikethrough: true;");

            Label savingsLabel = new Label("You Saved: " + currencyFormat.format(savedAmount));
            savingsLabel.setStyle("-fx-text-fill: #2ecc71; -fx-font-weight: bold;");

            Label finalTotalLabel = new Label("Final Total: " + currencyFormat.format(total));
            finalTotalLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");

            totalInfoBox.getChildren().addAll(originalTotalLabel, savingsLabel, finalTotalLabel);

            // Replace the total label with the VBox
            if (totalLabel.getParent() instanceof Pane) {
                Pane parent = (Pane) totalLabel.getParent();
                int index = parent.getChildren().indexOf(totalLabel);
                parent.getChildren().remove(totalLabel);
                parent.getChildren().add(index, totalInfoBox);
            } else {
                // Fallback if we can't replace the label
                totalLabel.setText("Total: " + currencyFormat.format(total) + " (You saved: " +
                        currencyFormat.format(savedAmount) + ")");
            }
        } else {
            // No discounts applied
            totalLabel.setText("Total: " + currencyFormat.format(total));
        }
    }

    @FXML
    private void handleContinueShopping() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/views/nour/product_client.fxml"));
            Parent root = loader.load();

            ProductFrontController controller = loader.getController();
            controller.setCurrentUserId(currentUserId);  // Make sure to pass the user ID

            Stage stage = (Stage) continueShoppingButton.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Products");
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to navigate to products view", e);
            showAlert("Error", "Failed to navigate to products view: " + e.getMessage());
        }
    }

    @FXML
    private void handleClearCart() {
        try {
            if (commandeService.clearCart(currentUserId)) {
                loadCartItems();
                showAlert("Success", "Cart cleared successfully.");
            } else {
                showAlert("Error", "Failed to clear cart.");
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error while clearing cart", e);
            showAlert("Error", "Database error: " + e.getMessage());
        }
    }

    @FXML
    private void handleCheckout() {
        if (currentCartItems == null || currentCartItems.isEmpty()) {
            showAlert("Warning", "Your cart is empty. Add some products first.");
            return;
        }

        try {
            // Convert cart items to product list
            List<Produit> productList = currentCartItems.stream()
                    .map(CartItem::getProduit)
                    .collect(Collectors.toList());

            // Create quantities map
            Map<Integer, Integer> quantitiesMap = new HashMap<>();
            for (CartItem item : currentCartItems) {
                quantitiesMap.put(item.getProduit().getId(), item.getQuantity());
            }

            // Calculate total
            double total = currentCartItems.stream()
                    .mapToDouble(CartItem::getSubtotal)
                    .sum();

            // Generate order reference
            String orderReference = generateOrderReference();

            // Navigate to confirmation page
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/views/nour/confirmation_purchase.fxml"));
            Parent root = loader.load();

            ConfirmationPurchaseController controller = loader.getController();
            controller.setCurrentUserId(currentUserId);
            controller.setCartProducts(productList);
            controller.setProductQuantities(quantitiesMap);
            controller.setOrderReference(orderReference);
            controller.setTotalAmount(currencyFormat.format(total));

            Stage stage = (Stage) checkoutButton.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Order Confirmation");

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to load confirmation view", e);
            showAlert("Error", "Failed to proceed to checkout. Please try again.");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error during checkout process", e);
            showAlert("Error", "Failed to process checkout: " + e.getMessage());
        }
    }

    private String generateOrderReference() {
        // Simple order reference format: ST-YYYYMMDD-XXXXX
        // Where XXXXX is a random 5-digit number
        String datePrefix = LocalDate.now().toString().replace("-", "");
        int randomNum = 10000 + (int)(Math.random() * 90000);
        return "ST-" + datePrefix + "-" + randomNum;
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // This method is kept for backward compatibility
    public void setCartProducts(List<Produit> cartProducts) {
        // Just refresh the cart items as we're now loading from database
        if (currentUserId > 0) {
            loadCartItems();
        }
    }

    private void navigateToLogin() {
        try {
            // Clear any existing auth data
            AuthManager.clearAll();

            // Navigate to login screen
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/views/login.fxml"));
            Parent root = loader.load();

            // Get the current scene/stage safely
            if (cartItemsContainer != null && cartItemsContainer.getScene() != null &&
                    cartItemsContainer.getScene().getWindow() != null) {

                Stage stage = (Stage) cartItemsContainer.getScene().getWindow();
                stage.setScene(new Scene(root));
                stage.setTitle("Login");
            } else {
                // If we can't get the current stage, create a new one
                LOGGER.info("Creating new stage for login navigation");
                Stage newStage = new Stage();
                newStage.setScene(new Scene(root));
                newStage.setTitle("Login");
                newStage.show();

                // Close the current stage if we can find it
                if (continueShoppingButton != null &&
                        continueShoppingButton.getScene() != null &&
                        continueShoppingButton.getScene().getWindow() != null) {
                    ((Stage) continueShoppingButton.getScene().getWindow()).close();
                } else if (checkoutButton != null &&
                        checkoutButton.getScene() != null &&
                        checkoutButton.getScene().getWindow() != null) {
                    ((Stage) checkoutButton.getScene().getWindow()).close();
                }
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to navigate to login screen", e);
            showAlert("Navigation Error", "Failed to navigate to login screen. Please restart the application.");
        }
    }
}