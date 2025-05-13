package com.controllers.nour;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class OrderSuccessController {

    private static final Logger LOGGER = Logger.getLogger(OrderSuccessController.class.getName());

    @FXML private Label orderReferenceLabel;
    @FXML private Label customerNameLabel;
    @FXML private Label customerEmailLabel;
    @FXML private Button continueShoppingButton;

    private String orderReference;
    private String customerName;
    private String customerEmail;

    @FXML
    public void initialize() {
        // Initialize UI components if needed
    }

    public void setOrderDetails(String orderReference, String customerName, String customerEmail) {
        this.orderReference = orderReference;
        this.customerName = customerName;
        this.customerEmail = customerEmail;

        // Update UI with order information
        if (orderReferenceLabel != null) {
            orderReferenceLabel.setText(orderReference);
        }

        if (customerNameLabel != null) {
            customerNameLabel.setText(customerName);
        }

        if (customerEmailLabel != null) {
            customerEmailLabel.setText(customerEmail);
        }
    }

    @FXML
    private void handleContinueShopping() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/views/nour/product_client.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) continueShoppingButton.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("SahaTech Products");
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to navigate to products view", e);
            showAlert("Error", "Failed to navigate to products view: " + e.getMessage());
        }
    }

    @FXML
    private void handleViewOrderDetails() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/views/nour/order_details_view.fxml"));
            Parent root = loader.load();

            // You would need to create an OrderDetailsController class
            // and pass the order reference to load the specific order details

            Stage stage = (Stage) continueShoppingButton.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Order Details");
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to navigate to order details", e);
            showAlert("Error", "Failed to load order details: " + e.getMessage());
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}