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
import com.exceptions.AuthException;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
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

    @FXML
    private Label authStatusLabel; // Added to show authentication status

    private CommandeService commandeService;
    private UserService userService;
    private AuthService authService;
    private User currentUser;
    private String token;
    private List<CartItem> currentCartItems;
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.US);
    private boolean needsNavigationToLogin = false;

    /**
     * Primary method for authentication - using token
     */
    public void setToken(String token) throws AuthException {
        this.token = token;

        if (token == null || token.isEmpty()) {
            LOGGER.warning("No token provided to CartViewController");
            needsNavigationToLogin = true;
            return;
        }

        try {
            // Get user from token
            currentUser = authService.getUserFromToken(token);

            if (currentUser == null || currentUser.getId() <= 0) {
                LOGGER.warning("Invalid user from token");
                needsNavigationToLogin = true;
                return;
            }

            LOGGER.info("Successfully authenticated user with token: " + currentUser.getEmail());

            // Token is already stored as a class variable - no SessionManager needed

            // Load cart data
            loadCartItems();

        } catch (AuthException e) {
            LOGGER.log(Level.WARNING, "Authentication error with token: " + e.getMessage(), e);
            this.token = null; // Clear the token locally
            this.currentUser = null;
            needsNavigationToLogin = true;
            throw e;
        }
    }

    @FXML
    public void initialize() {
        // Initialize services
        commandeService = new CommandeService();
        userService = new UserService();
        authService = new AuthService();

        // Default state - empty cart
        showEmptyCartMessage();

        // Don't load cart items here - wait for setToken to be called
    }

    /**
     * Check for pending navigation after the scene is fully loaded
     */
    public void checkPendingNavigation() {
        if (needsNavigationToLogin) {
            navigateToLogin();
            needsNavigationToLogin = false;
        }
    }

    private void loadCartItems() {
        try {
            if (currentUser == null || currentUser.getId() <= 0) {
                LOGGER.warning("Attempted to load cart items without valid user");
                showEmptyCartMessage();
                return;
            }

            LOGGER.info("Loading cart items for user ID: " + currentUser.getId());

            // Update authentication status label if visible
            if (authStatusLabel != null) {
                authStatusLabel.setText("Authenticated as: " + currentUser.getEmail());
            }

            currentCartItems = commandeService.getCartItems(currentUser.getId());
            cartItemsContainer.getChildren().clear();

            if (currentCartItems == null || currentCartItems.isEmpty()) {
                LOGGER.info("No items in cart for user ID: " + currentUser.getId());
                showEmptyCartMessage();
                checkoutButton.setDisable(true);
                totalLabel.setText("Total: 0.00 D");
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
            totalLabel.setText("Total: 0.00 D");
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

        // Price display - USE THE STORED PRICE_AT_PURCHASE instead of recalculating
        double originalPrice = item.getProduit().getPrice();
        double actualPrice = item.getPrice_at_purchase();
        boolean hasDiscount = actualPrice < originalPrice;

        // Create price display
        VBox priceDetailsBox = new VBox(3);
        priceDetailsBox.setAlignment(Pos.CENTER_LEFT);

        // Show original price (with strikethrough if discounted)
        if (hasDiscount) {
            // Original price with strikethrough
            Label originalPriceLabel = new Label("Original price: " + currencyFormat.format(originalPrice));
            originalPriceLabel.setStyle("-fx-text-fill: #7f8c8d; -fx-strikethrough: true;");

            // Final price with discount type
            Label finalPriceLabel = new Label(currencyFormat.format(actualPrice));
            finalPriceLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");

            // Calculate discount percentage
            double discountPercentage = ((originalPrice - actualPrice) / originalPrice) * 100;
            // Discount type label
            Label discountLabel = new Label(String.format("%.0f%% discount", discountPercentage));
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

        // Subtotal - use the actual price with discount applied
        double subtotal = actualPrice * item.getQuantity();
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
            if (commandeService.removeFromCart(currentUser.getId(), item.getProduit().getId())) {
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

        for (CartItem item : cartItems) {
            double originalPrice = item.getProduit().getPrice();
            double actualPrice = item.getPrice_at_purchase();

            // Calculate totals using the actual stored price
            originalTotal += originalPrice * item.getQuantity();
            total += actualPrice * item.getQuantity();
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
    private void handleBackToProducts() {
        try {
            // Charger le conteneur principal qui contient la navbar
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/views/event/front_main.fxml"));
            Parent root = loader.load();
            
            // Le root doit être un BorderPane
            if (root instanceof BorderPane) {
                BorderPane borderPane = (BorderPane) root;
                
                // Charger la vue des produits pour le centre
                FXMLLoader productLoader = new FXMLLoader(getClass().getResource("/com/views/nour/product_client.fxml"));
                Parent productView = productLoader.load();
                
                // Passer le token au contrôleur des produits
                try {
                    ProductFrontController productController = productLoader.getController();
                    if (token != null && !token.isEmpty()) {
                        try {
                            productController.setToken(token);
                        } catch (AuthException e) {
                            LOGGER.log(Level.WARNING, "Authentication error when navigating back to products", e);
                            showAlert("Authentication Error", "Your session may have expired. Please log in again.");
                            needsNavigationToLogin = true;
                            checkPendingNavigation();
                            return;
                        }
                    }
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Could not set token on ProductFrontController", e);
                }
                
                // Placer la vue des produits au centre
                borderPane.setCenter(productView);
                
                // Créer une nouvelle scène avec la navbar
                Scene scene = new Scene(root);
                
                // Ajouter les feuilles de style si nécessaire
                if (!scene.getStylesheets().contains("/com/styles/event/style.css")) {
                    scene.getStylesheets().add("/com/styles/event/style.css");
                }
                
                // Appliquer la scène
                Stage stage = (Stage) continueShoppingButton.getScene().getWindow();
                stage.setScene(scene);
                stage.setTitle("Products");
                
                // Passer l'utilisateur courant au contrôleur de la navbar si disponible
                try {
                    // Dans front_main.fxml, la navbar est incluse par un fx:include
                    // Nous devons accéder au contrôleur FrontMainController
                    Object mainController = loader.getController();
                    if (mainController != null && mainController instanceof com.event.controllers.FrontMainController) {
                        if (currentUser != null) {
                            // Créer l'utilisateur du bon type
                            com.event.models.User eventUser = new com.event.models.User();
                            eventUser.setId(currentUser.getId());
                            eventUser.setEmail(currentUser.getEmail());
                            
                            // Définir l'utilisateur dans le contrôleur principal
                            java.lang.reflect.Method setUserMethod = 
                                mainController.getClass().getMethod("setCurrentUser", com.event.models.User.class);
                            setUserMethod.invoke(mainController, eventUser);
                            
                            // Récupérer le contrôleur de la navbar et activer le bouton Produits
                            try {
                                java.lang.reflect.Method getNavbarControllerMethod = 
                                    mainController.getClass().getMethod("getNavbarController");
                                Object navbarController = getNavbarControllerMethod.invoke(mainController);
                                
                                if (navbarController != null) {
                                    java.lang.reflect.Method setActiveNavButtonMethod = 
                                        navbarController.getClass().getMethod("setActiveNavButton", String.class);
                                    setActiveNavButtonMethod.invoke(navbarController, "products");
                                }
                            } catch (Exception ex) {
                                LOGGER.log(Level.WARNING, "Could not set active nav button", ex);
                            }
                        }
                    }
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Could not set user on navbar controller: " + e.getMessage(), e);
                }
            } else {
                LOGGER.log(Level.WARNING, "Front main container root is not a BorderPane");
                // Fallback to direct loading
                FXMLLoader productLoader = new FXMLLoader(getClass().getResource("/com/views/nour/product_client.fxml"));
                Parent productView = productLoader.load();
                
                ProductFrontController controller = productLoader.getController();
                if (token != null && !token.isEmpty()) {
                    try {
                        controller.setToken(token);
                    } catch (AuthException e) {
                        LOGGER.log(Level.WARNING, "Authentication error when navigating back to products", e);
                        showAlert("Authentication Error", "Your session may have expired. Please log in again.");
                        needsNavigationToLogin = true;
                        checkPendingNavigation();
                    }
                }
                
                Stage stage = (Stage) continueShoppingButton.getScene().getWindow();
                stage.setScene(new Scene(productView));
                stage.setTitle("Products");
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to navigate back to products view", e);
            showAlert("Error", "Failed to navigate back: " + e.getMessage());
        }
    }
    
    // Gardé pour la compatibilité avec le code existant
    @FXML
    private void handleContinueShopping() {
        handleBackToProducts();
    }

    @FXML
    private void handleClearCart() {
        try {
            if (currentUser == null || currentUser.getId() <= 0) {
                showAlert("Error", "Authentication required");
                return;
            }

            if (commandeService.clearCart(currentUser.getId())) {
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
        if (currentUser == null || currentUser.getId() <= 0) {
            showAlert("Error", "Authentication required");
            needsNavigationToLogin = true;
            checkPendingNavigation();
            return;
        }

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

            // Calculate total - use the discounted prices stored in CartItem
            double total = currentCartItems.stream()
                    .mapToDouble(item -> item.getPrice_at_purchase() * item.getQuantity())
                    .sum();

            // Generate order reference
            String orderReference = generateOrderReference();

            // Navigate to confirmation page
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/views/nour/confirmation_purchase.fxml"));
            Parent root = loader.load();

            ConfirmationPurchaseController controller = loader.getController();
            controller.setCurrentUserId(currentUser.getId());
            controller.setCartProducts(productList);
            controller.setProductQuantities(quantitiesMap);
            controller.setOrderReference(orderReference);
            controller.setTotalAmount(currencyFormat.format(total));

            // Pass the token if the controller supports it
            try {
                if (token != null && !token.isEmpty()) {
                    java.lang.reflect.Method setTokenMethod = controller.getClass().getMethod("setToken", String.class);
                    setTokenMethod.invoke(controller, token);
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Could not set token on ConfirmationPurchaseController", e);
            }

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

    /**
     * Legacy method for backward compatibility
     * You should use setToken instead for new code
     */
    @Deprecated
    public void setCartProducts(List<Produit> cartProducts) {
        // Just refresh the cart items - this is maintained only for backwards compatibility
        if (currentUser != null && currentUser.getId() > 0) {
            loadCartItems();
        }
    }

    /**
     * Legacy method for backward compatibility
     * You should use setToken instead for new code
     */
    @Deprecated
    public void setCurrentUserId(int userId) {
        LOGGER.warning("setCurrentUserId is deprecated. Use setToken instead.");

        if (userId <= 0) {
            needsNavigationToLogin = true;
            return;
        }

        try {
            this.currentUser = userService.rechercherUserParId(userId);
            if (this.currentUser != null) {
                loadCartItems();
            } else {
                needsNavigationToLogin = true;
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error loading user data for legacy method", e);
            needsNavigationToLogin = true;
        }
    }

    private void navigateToLogin() {
        try {
            this.token = null;
            this.currentUser = null;

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

    /**
     * Public method to refresh authentication if needed
     */
    public void refreshAuthentication() {
        if (token != null && !token.isEmpty()) {
            try {
                setToken(token);
            } catch (AuthException e) {
                LOGGER.log(Level.WARNING, "Failed to refresh authentication", e);
                needsNavigationToLogin = true;
                checkPendingNavigation();
            }
        } else {
            LOGGER.warning("Cannot refresh authentication - no token available");
            needsNavigationToLogin = true;
            checkPendingNavigation();
        }
    }
}