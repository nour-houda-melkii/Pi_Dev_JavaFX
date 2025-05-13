package com.controllers.nour;

import com.models.Produit;
import com.services.PaymentService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import java.io.IOException;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class StripePaymentController {

    private static final Logger LOGGER = Logger.getLogger(StripePaymentController.class.getName());

    @FXML private Label orderReferenceLabel;
    @FXML private Label totalAmountLabel;
    @FXML private Label customerNameLabel;
    @FXML private Label customerEmailLabel;
    @FXML private Button useStripeCheckoutButton;

    private final PaymentService paymentService;
    private List<Produit> cartProducts;
    private Map<Integer, Integer> productQuantities;
    private String orderReference;
    private String totalAmount;
    private String customerEmail;
    private String customerName;

    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.US);

    public StripePaymentController() {
        this.paymentService = new PaymentService();
    }

    @FXML
    public void initialize() {
        // Initialization if needed
    }

    public void setPaymentInfo(List<Produit> cartProducts, Map<Integer, Integer> productQuantities,
                               String orderReference, String totalAmount, String customerEmail,
                               String customerName, String customerAddress, String customerPhone,
                               String customerPassword) {
        this.cartProducts = cartProducts;
        this.productQuantities = productQuantities;
        this.orderReference = orderReference;
        this.totalAmount = totalAmount;
        this.customerEmail = customerEmail;
        this.customerName = customerName;

        // Update UI with order information
        if (orderReferenceLabel != null) {
            orderReferenceLabel.setText(orderReference);
        }

        if (totalAmountLabel != null) {
            totalAmountLabel.setText(totalAmount);
        }

        if (customerNameLabel != null) {
            customerNameLabel.setText(customerName);
        }

        if (customerEmailLabel != null) {
            customerEmailLabel.setText(customerEmail);
        }
    }

    @FXML
    private void handleStripeCheckout() {
        try {
            String amountStr = totalAmount.replaceAll("[^\\d.]", "");
            double amount = Double.parseDouble(amountStr);

            String checkoutUrl = paymentService.createStripeCheckoutSession(
                    amount, orderReference, customerName, customerEmail);

            openInBrowser(checkoutUrl);

            showAlert("Stripe Checkout", "Please complete your payment in the browser window that has opened. " +
                    "After payment, please return to this application.");

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to create Stripe Checkout session", e);
            showAlert("Error", "Failed to create payment session: " + e.getMessage());
        }
    }

    private void openInBrowser(String url) throws IOException {
        java.awt.Desktop.getDesktop().browse(java.net.URI.create(url));
    }

    @FXML
    private void handleBackToConfirmation() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/views/nour/confirmation_purchase.fxml"));
            Parent root = loader.load();

            ConfirmationPurchaseController controller = loader.getController();
            controller.setCartProducts(cartProducts);
            controller.setProductQuantities(productQuantities);
            controller.setOrderReference(orderReference);
            controller.setTotalAmount(totalAmount);

            Stage stage = (Stage) useStripeCheckoutButton.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Confirm Purchase");
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to navigate back to confirmation", e);
            showAlert("Error", "Failed to navigate back: " + e.getMessage());
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