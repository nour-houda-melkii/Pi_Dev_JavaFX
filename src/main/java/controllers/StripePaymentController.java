package controllers;

import Services.PaymentService;
import com.stripe.exception.StripeException;
import entities.Produit;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
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

    @FXML private TextField cardNumberField;
    @FXML private TextField expiryMonthField;
    @FXML private TextField expiryYearField;
    @FXML private TextField cvcField;

    @FXML private Button payNowButton;
    @FXML private Button useStripeCheckoutButton;

    private final PaymentService paymentService;
    private List<Produit> cartProducts;
    private Map<Integer, Integer> productQuantities;
    private String orderReference;
    private String totalAmount;
    private String customerEmail;
    private String customerName;
    private String customerAddress;
    private String customerPhone;
    private String customerPassword;

    // For formatting currency
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.US);

    public StripePaymentController() {
        this.paymentService = new PaymentService();
    }

    @FXML
    public void initialize() {
        // Initialize UI components if needed
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
        this.customerAddress = customerAddress;
        this.customerPhone = customerPhone;
        this.customerPassword = customerPassword;

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
            // Get payment amount (remove currency symbol and parse)
            String amountStr = totalAmount.replaceAll("[^\\d.]", "");
            double amount = Double.parseDouble(amountStr);

            // Create Stripe Checkout session
            String checkoutUrl = paymentService.createStripeCheckoutSession(
                    amount, orderReference, customerName, customerEmail);

            // Open checkout URL in browser
            openInBrowser(checkoutUrl);

            // Show instructions to user
            showAlert("Stripe Checkout", "Please complete your payment in the browser window that has opened. " +
                    "After payment, please return to this application.");

        } catch (StripeException e) {
            LOGGER.log(Level.SEVERE, "Failed to create Stripe Checkout session", e);
            showAlert("Error", "Failed to create payment session: " + e.getMessage());
        } catch (NumberFormatException e) {
            LOGGER.log(Level.SEVERE, "Invalid amount format", e);
            showAlert("Error", "Invalid payment amount.");
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to open browser", e);
            showAlert("Error", "Failed to open payment page in browser: " + e.getMessage());
        }
    }

    private void openInBrowser(String url) throws IOException {
        java.awt.Desktop.getDesktop().browse(java.net.URI.create(url));
    }

    private boolean validatePaymentFields() {
        // Basic card number validation (16 digits)
        if (cardNumberField.getText().isEmpty() || !cardNumberField.getText().matches("\\d{16}")) {
            showAlert("Invalid Card Number", "Please enter a valid 16-digit card number.");
            return false;
        }

        // Expiry month validation (1-12)
        try {
            int month = Integer.parseInt(expiryMonthField.getText());
            if (month < 1 || month > 12) {
                showAlert("Invalid Expiry Month", "Month must be between 1 and 12.");
                return false;
            }
        } catch (NumberFormatException e) {
            showAlert("Invalid Expiry Month", "Please enter a valid month (1-12).");
            return false;
        }

        // Expiry year validation (current year or later)
        try {
            int year = Integer.parseInt(expiryYearField.getText());
            int currentYear = java.time.Year.now().getValue() % 100; // Get last two digits
            if (year < currentYear) {
                showAlert("Invalid Expiry Year", "Year must be current year or later.");
                return false;
            }
        } catch (NumberFormatException e) {
            showAlert("Invalid Expiry Year", "Please enter a valid year.");
            return false;
        }

        // CVC validation (3-4 digits)
        if (cvcField.getText().isEmpty() || !cvcField.getText().matches("\\d{3,4}")) {
            showAlert("Invalid CVC", "Please enter a valid CVC (3-4 digits).");
            return false;
        }

        return true;
    }

    private void updateOrderStatus() {
        // Update order status in database (implementation would depend on your data layer)
        // For now, just log the action
        LOGGER.info("Order " + orderReference + " marked as paid");
    }

    private void navigateToOrderSuccessPage() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/order_success_view.fxml"));
            Parent root = loader.load();

            OrderSuccessController controller = loader.getController();
            controller.setOrderDetails(orderReference, customerName, customerEmail);

            Stage stage = (Stage) cardNumberField.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Order Successful");
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to navigate to success page", e);
            showAlert("Error", "Failed to navigate to success page: " + e.getMessage());

            // As fallback, navigate to product view
            navigateToProductView();
        }
    }

    @FXML
    private void handleBackToConfirmation() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/confiramtion_purchase.fxml"));
            Parent root = loader.load();

            ConfirmationPurchaseController controller = loader.getController();
            controller.setCartProducts(cartProducts);
            controller.setProductQuantities(productQuantities);
            controller.setOrderReference(orderReference);
            controller.setTotalAmount(totalAmount);

            Stage stage = (Stage) cardNumberField.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Confirm Purchase");
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to navigate back to confirmation", e);
            showAlert("Error", "Failed to navigate back: " + e.getMessage());
        }
    }

    private void navigateToProductView() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/product_client.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) cardNumberField.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("SahaTech Products");
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to navigate to products view", e);
            showAlert("Error", "Failed to navigate to products view: " + e.getMessage());
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