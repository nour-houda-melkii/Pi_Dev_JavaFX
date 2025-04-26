package com.controllers.nour;

import com.models.*;
import com.services.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
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
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

public class CartViewController {

    @FXML private FlowPane cartProductContainer;
    @FXML private Label totalAmountLabel;

    private List<Produit> cartProducts;
    private Map<Integer, Integer> productQuantities = new HashMap<>();
    private String orderReference;
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.US);

    @FXML
    public void initialize() {
        // Generate a unique order reference on initialization
        orderReference = generateOrderReference();
    }

    private String generateOrderReference() {
        return "ORD-" + System.currentTimeMillis() + "-" + new Random().nextInt(1000);
    }

    private void updateTotal() {
        double total = 0;
        for (Produit product : cartProducts) {
            int quantity = productQuantities.getOrDefault(product.getId(), 1);
            total += product.getPrice() * quantity;
        }
        totalAmountLabel.setText(currencyFormat.format(total));
    }

    @FXML
    private void handleConfirmPurchase() {
        if (cartProducts.isEmpty()) {
            showAlert("Cart Empty", "Your cart is empty. Add some products first!");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/views/nour/confirmation_purchase.fxml"));
            Parent root = loader.load();

            ConfirmationPurchaseController controller = loader.getController();
            controller.setCartProducts(cartProducts);
            controller.setProductQuantities(productQuantities);
            controller.setOrderReference(orderReference);
            controller.setTotalAmount(totalAmountLabel.getText());

            // Replace the current scene
            Stage stage = (Stage) totalAmountLabel.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Confirm Purchase");
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to navigate to confirmation page: " + e.getMessage());
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public void setCartProducts(List<Produit> cartProducts) {
        this.cartProducts = cartProducts;
        displayProductsInCardView();
        updateTotal();
    }

    private void displayProductsInCardView() {
        cartProductContainer.getChildren().clear();

        for (Produit product : cartProducts) {
            cartProductContainer.getChildren().add(createProductCard(product));
        }
    }

    private VBox createProductCard(Produit product) {
        // Create a styled card container
        VBox card = new VBox(10);
        card.setPrefWidth(200);
        card.setPrefHeight(320);
        card.setStyle("-fx-background-color: white; " +
                "-fx-border-color: #f0f0f0; " +
                "-fx-border-radius: 8; " +
                "-fx-border-color: black; " +
                "-fx-border-width: 1.5; " +
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

        int quantity = productQuantities.getOrDefault(product.getId(), 1);
        double totalPrice = product.getPrice() * quantity;

        Label priceLabel = new Label(String.format("Price: %s", currencyFormat.format(product.getPrice())));
        priceLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #33ccff; -fx-padding: 0 10 0 10;");

        Label quantityLabel = new Label(String.format("Quantity: %d", quantity));
        quantityLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #333333; -fx-padding: 0 10 0 10;");

        Label totalLabel = new Label(String.format("Total: %s", currencyFormat.format(totalPrice)));
        totalLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #27ae60; -fx-padding: 0 10 5 10;");

        // Quantity control buttons
        Button decreaseButton = new Button("-");
        decreaseButton.setStyle("-fx-background-color: #f0f0f0; -fx-text-fill: #333333; -fx-min-width: 30px;");

        Button increaseButton = new Button("+");
        increaseButton.setStyle("-fx-background-color: #33ccff; -fx-text-fill: white; -fx-min-width: 30px;");

        HBox quantityControlBox = new HBox(10, decreaseButton, quantityLabel, increaseButton);
        quantityControlBox.setStyle("-fx-alignment: center; -fx-padding: 5 10 5 10;");

        // Set up quantity control handlers
        decreaseButton.setOnAction(e -> {
            int currentQuantity = productQuantities.getOrDefault(product.getId(), 1);
            if (currentQuantity > 1) {
                productQuantities.put(product.getId(), currentQuantity - 1);
                updateCardAndTotal(product);
            }
        });

        increaseButton.setOnAction(e -> {
            int currentQuantity = productQuantities.getOrDefault(product.getId(), 1);
            productQuantities.put(product.getId(), currentQuantity + 1);
            updateCardAndTotal(product);
        });

        // Remove Button
        Button removeButton = new Button("Remove");
        removeButton.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-background-radius: 4;");
        removeButton.setOnAction(e -> {
            cartProducts.removeIf(p -> p.getId() == product.getId());
            productQuantities.remove(product.getId());
            displayProductsInCardView();
            updateTotal();
        });

        HBox buttonBox = new HBox(removeButton);
        buttonBox.setStyle("-fx-alignment: center; -fx-padding: 5 10 10 10;");

        // Add components to card
        card.getChildren().addAll(imageContainer, nameLabel, priceLabel, quantityControlBox, totalLabel, buttonBox);

        return card;
    }

    private void updateCardAndTotal(Produit product) {
        displayProductsInCardView();
        updateTotal();
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

    @FXML
    private void handleClearCart() {
        cartProducts.clear();
        productQuantities.clear();
        cartProductContainer.getChildren().clear();
        updateTotal();
    }

    @FXML
    private void handleBackToProducts() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/views/nour/product_client.fxml"));
            Parent root = loader.load();

            ProductFrontController controller = loader.getController();
            // Pass the current cart data back to the product view
            controller.setCartProducts(cartProducts);

            Stage stage = (Stage) cartProductContainer.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("SahaTech Products");
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to navigate back to products view: " + e.getMessage());
        }
    }
}