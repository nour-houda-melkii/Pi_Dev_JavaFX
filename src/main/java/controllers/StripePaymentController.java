package controllers;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import entities.Produit;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import netscape.javascript.JSObject;

import java.io.IOException;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

public class StripePaymentController {

    private static final Logger LOGGER = Logger.getLogger(StripePaymentController.class.getName());

    @FXML private WebView paymentWebView;
    @FXML private Label totalAmountLabel;
    @FXML private Label orderReferenceLabel;
    @FXML private TextField cardholderNameField;
    @FXML private ProgressIndicator progressIndicator;
    @FXML private VBox paymentFormContainer;

    private List<Produit> cartProducts;
    private Map<Integer, Integer> productQuantities = new HashMap<>();
    private String orderReference;
    private String totalAmount;
    private double amountInCents;
    private String customerEmail;
    private String customerName;
    private String customerAddress;
    private String customerPhone;
    private String customerPassword;

    // Stripe API key configuration - ideally these should be loaded from config file or environment variables
    private static final String STRIPE_API_KEY = System.getenv("STRIPE_API_KEY") != null ?
            System.getenv("STRIPE_API_KEY") : "sk_test_51Qz2QF4YsQuztlT92U6B5YdwomnTnntKDA5J1eBR7KOzc47jKGmMUIWBbj7VFokp0MSsdZiHFtuE5grWpjbzq7dh00GyIThE5k";
    private static final String STRIPE_PUBLIC_KEY = System.getenv("STRIPE_PUBLIC_KEY") != null ?
            System.getenv("STRIPE_PUBLIC_KEY") : "pk_test_51Qz2QF4YsQuztlT91g6R4m8lfwx8w1jZ18bHyUkIeRxT4vI62IU0dCcxZ3plPrgL5Y4E6UkHW0LeaaxqwnN5v6g300h5FJU0wA";

    @FXML
    public void initialize() {
        // Initialize Stripe API with your secret key
        Stripe.apiKey = STRIPE_API_KEY;

        // Hide progress indicator initially
        progressIndicator.setVisible(false);

        LOGGER.info("StripePaymentController initialized");
    }

    public void setPaymentInfo(List<Produit> cartProducts, Map<Integer, Integer> productQuantities,
                               String orderReference, String totalAmount, String email,
                               String name, String address, String phone, String password) {
        this.cartProducts = cartProducts;
        this.productQuantities = productQuantities;
        this.orderReference = orderReference;
        this.totalAmount = totalAmount;
        this.customerEmail = email;
        this.customerName = name;
        this.customerAddress = address;
        this.customerPhone = phone;
        this.customerPassword = password;

        // Prefill cardholder name if provided
        if (name != null && !name.isEmpty()) {
            cardholderNameField.setText(name);
        }

        // Convert total amount string (e.g. "$120.50") to cents for Stripe
        String amountStr = totalAmount.replaceAll("[^\\d.]", "");
        double amountInDollars = Double.parseDouble(amountStr);
        this.amountInCents = Math.round(amountInDollars * 100);

        // Update UI elements
        totalAmountLabel.setText(totalAmount);
        orderReferenceLabel.setText(orderReference);

        // Log payment details for debugging
        LOGGER.info("Payment info set: Order=" + orderReference + ", Amount=" + totalAmount +
                ", AmountInCents=" + amountInCents + ", Customer=" + customerEmail);

        // Load Stripe Elements
        initializeStripePaymentForm();
    }

    private void initializeStripePaymentForm() {
        WebEngine webEngine = paymentWebView.getEngine();

        // Load HTML with Stripe Elements
        String stripeHtml = generateStripeHtml();
        webEngine.loadContent(stripeHtml);

        // Add JavaScript bridge to handle callbacks
        webEngine.getLoadWorker().stateProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == Worker.State.SUCCEEDED) {
                LOGGER.info("WebView loaded successfully");
                JSObject window = (JSObject) webEngine.executeScript("window");
                window.setMember("javaConnector", new JavaConnector());

                // Update cardholder name in JavaScript
                if (customerName != null && !customerName.isEmpty()) {
                    webEngine.executeScript("window.setCardholderName('" + customerName.replace("'", "\\'") + "');");
                    LOGGER.info("Set cardholder name in JS: " + customerName);
                }

                // Add console logger
                webEngine.executeScript(
                        "console.log = function(message) { " +
                                "    if(window.javaConnector) { " +
                                "        window.javaConnector.log(message); " +
                                "    } else { " +
                                "        alert('JavaConnector not available: ' + message); " +
                                "    }" +
                                "};"
                );

                LOGGER.info("JavaScript bridge setup complete");
            }
        });
    }

    // JavaScript connector class
    public class JavaConnector {
        public void processPayment(String paymentMethodId) {
            LOGGER.info("JavaConnector.processPayment called with ID: " + paymentMethodId);
            System.out.println("JavaConnector.processPayment called with ID: " + paymentMethodId);

            Platform.runLater(() -> {
                LOGGER.info("Processing payment with method ID: " + paymentMethodId);
                startPaymentProcess(paymentMethodId);
            });
        }

        public void handleError(String errorMessage) {
            LOGGER.warning("JavaConnector.handleError called: " + errorMessage);
            System.out.println("JavaConnector.handleError called: " + errorMessage);

            Platform.runLater(() -> {
                LOGGER.warning("Payment error: " + errorMessage);
                progressIndicator.setVisible(false);
                paymentFormContainer.setDisable(false);
                showAlert("Payment Error", errorMessage);
            });
        }

        public void log(String message) {
            LOGGER.info("JS Console: " + message);
            System.out.println("JS Console: " + message);
        }
    }

    private void startPaymentProcess(String paymentMethodId) {
        // Show processing indicator
        progressIndicator.setVisible(true);
        paymentFormContainer.setDisable(true);

        LOGGER.info("Starting payment process...");

        // Add timeout check
        CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(30000); // 30 seconds timeout
                Platform.runLater(() -> {
                    if (progressIndicator.isVisible()) {
                        LOGGER.warning("Payment process timed out after 30 seconds");
                        progressIndicator.setVisible(false);
                        paymentFormContainer.setDisable(false);
                        showAlert("Payment Timeout", "Payment processing is taking longer than expected. Please try again.");
                    }
                });
            } catch (InterruptedException e) {
                LOGGER.log(Level.SEVERE, "Payment timeout interrupted", e);
            }
        });

        // Validate cardholder name
        String cardholderName = cardholderNameField.getText().trim();
        if (cardholderName.isEmpty()) {
            progressIndicator.setVisible(false);
            paymentFormContainer.setDisable(false);
            showAlert("Missing Information", "Please enter cardholder name.");
            return;
        }

        // Process payment in background
        CompletableFuture.supplyAsync(() -> {
            try {
                LOGGER.info("Creating payment intent with Stripe API");

                // Create a PaymentIntent with Stripe API
                PaymentIntentCreateParams.Builder paramsBuilder = PaymentIntentCreateParams.builder()
                        .setCurrency("eur")
                        .setAmount(Math.round(amountInCents))
                        .putMetadata("order_reference", orderReference)
                        .putMetadata("customer_email", customerEmail)
                        .putMetadata("customer_name", customerName)
                        .setReceiptEmail(customerEmail)
                        .setConfirm(true)
                        .setPaymentMethod(paymentMethodId);

                // Add description
                paramsBuilder.setDescription("SahaTech Order: " + orderReference);

                // Add shipping information if available
                if (customerAddress != null && !customerAddress.isEmpty()) {
                    PaymentIntentCreateParams.Shipping shipping = PaymentIntentCreateParams.Shipping.builder()
                            .setName(customerName)
                            .setAddress(PaymentIntentCreateParams.Shipping.Address.builder()
                                    .setLine1(customerAddress)
                                    .setCountry("TN") // Assuming Tunisia based on the currency and project context
                                    .build())
                            .setPhone(customerPhone)
                            .build();
                    paramsBuilder.setShipping(shipping);
                }

                PaymentIntentCreateParams createParams = paramsBuilder.build();

                // Log the parameters before creating payment intent
                LOGGER.info("Creating payment intent with amount: " + createParams.getAmount() +
                        " currency: " + createParams.getCurrency());

                // Create and confirm the PaymentIntent
                PaymentIntent intent = PaymentIntent.create(createParams);

                LOGGER.info("Payment intent created: " + intent.getId() + ", status: " + intent.getStatus());
                System.out.println("Payment intent created: " + intent.getId() + ", status: " + intent.getStatus());

                // Check payment status
                String status = intent.getStatus();
                boolean success = "succeeded".equals(status) || "processing".equals(status);

                // If payment is still processing, try to retrieve the latest status
                if ("processing".equals(status)) {
                    LOGGER.info("Payment is processing. Waiting 5 seconds to check status again...");
                    try {
                        Thread.sleep(5000); // Wait 5 seconds
                        PaymentIntent updatedIntent = PaymentIntent.retrieve(intent.getId());
                        LOGGER.info("Updated payment status: " + updatedIntent.getStatus());
                        success = "succeeded".equals(updatedIntent.getStatus()) || "processing".equals(updatedIntent.getStatus());
                    } catch (InterruptedException e) {
                        LOGGER.log(Level.WARNING, "Sleep interrupted while waiting for payment status", e);
                    }
                }

                return success;

            } catch (StripeException e) {
                LOGGER.log(Level.SEVERE, "Stripe payment processing error", e);
                System.out.println("Stripe error: " + e.getMessage());
                Platform.runLater(() -> {
                    showAlert("Payment Processing Error", e.getMessage());
                });
                return false;
            }
        }).thenAccept(success -> {
            Platform.runLater(() -> {
                progressIndicator.setVisible(false);

                if (success) {
                    LOGGER.info("Payment successful, proceeding with order confirmation");
                    // Payment successful, proceed with order confirmation
                    completeOrderAfterPayment();
                } else {
                    LOGGER.warning("Payment failed or was declined");
                    // Payment failed
                    paymentFormContainer.setDisable(false);
                    showAlert("Payment Failed", "There was an error processing your payment. Please try again.");
                }
            });
        }).exceptionally(ex -> {
            LOGGER.log(Level.SEVERE, "Unexpected error during payment processing", ex);
            Platform.runLater(() -> {
                progressIndicator.setVisible(false);
                paymentFormContainer.setDisable(false);
                showAlert("Payment Error", "An unexpected error occurred: " + ex.getMessage());
            });
            return null;
        });
    }

    private String generateStripeHtml() {
        return "<!DOCTYPE html>\n" +
                "<html>\n" +
                "<head>\n" +
                "    <meta charset=\"utf-8\">\n" +
                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n" +
                "    <title>Stripe Payment</title>\n" +
                "    <script src=\"https://js.stripe.com/v3/\"></script>\n" +
                "    <style>\n" +
                "        body { font-family: 'Helvetica Neue', Helvetica, sans-serif; font-size: 16px; }\n" +
                "        .container { max-width: 500px; margin: 0 auto; }\n" +
                "        #card-element { margin-bottom: 24px; padding: 12px; border: 1px solid #e6e6e6; border-radius: 4px; }\n" +
                "        button { background: #5469d4; color: #ffffff; font-family: Arial, sans-serif; border-radius: 4px; border: 0; padding: 12px 16px; font-size: 16px; font-weight: 600; cursor: pointer; display: block; transition: all 0.2s ease; box-shadow: 0px 4px 5.5px 0px rgba(0, 0, 0, 0.07); width: 100%; }\n" +
                "        button:hover { filter: contrast(115%); }\n" +
                "        button:disabled { opacity: 0.5; cursor: default; }\n" +
                "        .spinner, .spinner:before, .spinner:after { border-radius: 50%; }\n" +
                "        .spinner { color: #ffffff; font-size: 22px; text-indent: -99999px; margin: 0px auto; position: relative; width: 20px; height: 20px; box-shadow: inset 0 0 0 2px; -webkit-transform: translateZ(0); -ms-transform: translateZ(0); transform: translateZ(0); }\n" +
                "        .spinner:before, .spinner:after { position: absolute; content: ''; }\n" +
                "        .spinner:before { width: 10.4px; height: 20.4px; background: #5469d4; border-radius: 20.4px 0 0 20.4px; top: -0.2px; left: -0.2px; -webkit-transform-origin: 10.4px 10.2px; transform-origin: 10.4px 10.2px; -webkit-animation: loading 2s infinite ease 1.5s; animation: loading 2s infinite ease 1.5s; }\n" +
                "        .spinner:after { width: 10.4px; height: 10.2px; background: #5469d4; border-radius: 0 10.2px 10.2px 0; top: -0.1px; left: 10.2px; -webkit-transform-origin: 0px 10.2px; transform-origin: 0px 10.2px; -webkit-animation: loading 2s infinite ease; animation: loading 2s infinite ease; }\n" +
                "        @keyframes loading { 0% { -webkit-transform: rotate(0deg); transform: rotate(0deg); } 100% { -webkit-transform: rotate(360deg); transform: rotate(360deg); }}\n" +
                "        #card-error { color: #dc3545; text-align: left; font-size: 13px; line-height: 17px; margin-top: 12px; }\n" +
                "    </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "    <div class=\"container\">\n" +
                "        <form id=\"payment-form\">\n" +
                "            <div id=\"card-element\"></div>\n" +
                "            <button id=\"submit-button\">\n" +
                "                <div class=\"spinner hidden\" id=\"spinner\"></div>\n" +
                "                <span id=\"button-text\">Pay " + totalAmount + "</span>\n" +
                "            </button>\n" +
                "            <div id=\"card-error\" role=\"alert\"></div>\n" +
                "        </form>\n" +
                "    </div>\n" +
                "    <script>\n" +
                "        console.log('Stripe payment form initializing...');\n" +
                "        var stripe = Stripe('" + STRIPE_PUBLIC_KEY + "');\n" +
                "        var elements = stripe.elements();\n" +
                "        var style = {\n" +
                "            base: {\n" +
                "                color: '#32325d',\n" +
                "                fontFamily: '\"Helvetica Neue\", Helvetica, sans-serif',\n" +
                "                fontSmoothing: 'antialiased',\n" +
                "                fontSize: '16px',\n" +
                "                '::placeholder': {color: '#aab7c4'}\n" +
                "            },\n" +
                "            invalid: {\n" +
                "                color: '#fa755a',\n" +
                "                iconColor: '#fa755a'\n" +
                "            }\n" +
                "        };\n" +
                "        \n" +
                "        // Create card Element and mount it to the DOM\n" +
                "        var card = elements.create('card', {\n" +
                "            style: style,\n" +
                "            hidePostalCode: false\n" +
                "        });\n" +
                "        card.mount('#card-element');\n" +
                "        console.log('Card element mounted');\n" +
                "\n" +
                "        // Handle real-time validation errors from the card Element\n" +
                "        card.on('change', function(event) {\n" +
                "            var displayError = document.getElementById('card-error');\n" +
                "            if (event.error) {\n" +
                "                displayError.textContent = event.error.message;\n" +
                "                console.log('Card validation error: ' + event.error.message);\n" +
                "            } else {\n" +
                "                displayError.textContent = '';\n" +
                "            }\n" +
                "        });\n" +
                "\n" +
                "        // Handle form submission\n" +
                "        var form = document.getElementById('payment-form');\n" +
                "        var submitButton = document.getElementById('submit-button');\n" +
                "        \n" +
                "        form.addEventListener('submit', function(ev) {\n" +
                "            ev.preventDefault();\n" +
                "            console.log('Form submitted, processing payment...');\n" +
                "            \n" +
                "            // Disable the submit button to prevent repeated clicks\n" +
                "            submitButton.disabled = true;\n" +
                "            submitButton.innerHTML = '<div class=\"spinner\" id=\"spinner\"></div><span id=\"button-text\">Processing...</span>';\n" +
                "            \n" +
                "            // Create payment method\n" +
                "            stripe.createPaymentMethod({\n" +
                "                type: 'card',\n" +
                "                card: card,\n" +
                "                billing_details: {\n" +
                "                    name: window.cardholderName || ''\n" +
                "                }\n" +
                "            }).then(function(result) {\n" +
                "                if (result.error) {\n" +
                "                    // Show error in payment form\n" +
                "                    console.log('Error creating payment method: ' + result.error.message);\n" +
                "                    var errorElement = document.getElementById('card-error');\n" +
                "                    errorElement.textContent = result.error.message;\n" +
                "                    submitButton.disabled = false;\n" +
                "                    submitButton.innerHTML = '<span id=\"button-text\">Pay " + totalAmount + "</span>';\n" +
                "                    \n" +
                "                    // Also notify Java about the error\n" +
                "                    try {\n" +
                "                        window.javaConnector.handleError(result.error.message);\n" +
                "                        console.log('Java connector notified about error');\n" +
                "                    } catch (e) {\n" +
                "                        console.log('Failed to notify Java connector about error: ' + e);\n" +
                "                    }\n" +
                "                } else {\n" +
                "                    // Payment method created - call JavaFX bridge\n" +
                "                    console.log('Payment method created: ' + result.paymentMethod.id);\n" +
                "                    console.log('Attempting to call Java connector...');\n" +
                "                    try {\n" +
                "                        window.javaConnector.processPayment(result.paymentMethod.id);\n" +
                "                        console.log('Java connector called successfully');\n" +
                "                    } catch (e) {\n" +
                "                        console.log('Error calling Java connector: ' + e);\n" +
                "                        alert('Error communicating with payment system: ' + e);\n" +
                "                        submitButton.disabled = false;\n" +
                "                        submitButton.innerHTML = '<span id=\"button-text\">Pay " + totalAmount + "</span>';\n" +
                "                    }\n" +
                "                }\n" +
                "            });\n" +
                "        });\n" +
                "        \n" +
                "        // Connect cardholder name from Java\n" +
                "        window.setCardholderName = function(name) {\n" +
                "            window.cardholderName = name;\n" +
                "            console.log('Cardholder name set to: ' + name);\n" +
                "        };\n" +
                "        \n" +
                "        console.log('Stripe payment form initialization complete');\n" +
                "    </script>\n" +
                "</body>\n" +
                "</html>";
    }

    private void completeOrderAfterPayment() {
        try {
            LOGGER.info("Completing order after successful payment");
            // Create a new instance of ConfirmationPurchaseController
            ConfirmationPurchaseController confirmationController = new ConfirmationPurchaseController();

            // Send confirmation email with all customer and order details
            boolean emailSent = confirmationController.sendConfirmationEmail(
                    customerEmail, customerPassword, customerName, customerAddress,
                    customerPhone, orderReference, totalAmount, cartProducts, productQuantities);

            if (emailSent) {
                LOGGER.info("Confirmation email sent successfully to: " + customerEmail);
            } else {
                LOGGER.warning("Failed to send confirmation email to: " + customerEmail);
            }

            // Show success message
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Payment Successful");
            alert.setHeaderText("Thank you for your purchase!");
            alert.setContentText("Payment has been processed successfully.\n\n" +
                    "Order Reference: " + orderReference + "\n" +
                    "Total amount: " + totalAmount + "\n\n" +
                    (emailSent ? "A confirmation email has been sent to your email address." :
                            "We couldn't send a confirmation email. Please contact support."));
            alert.showAndWait();

            // Show QR code with order details
            showQRCode();

            // Navigate back to product view and clear the cart
            navigateToProductView(true);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error completing order after payment", e);
            showAlert("Error", "Payment was successful but we couldn't process your order: " + e.getMessage());
        }
    }

    private void showQRCode() {
        try {
            StringBuilder qrContent = new StringBuilder();
            qrContent.append("Order: ").append(orderReference).append("\n");
            qrContent.append("Customer: ").append(customerName).append("\n");
            qrContent.append("Total: ").append(totalAmount).append("\n");
            qrContent.append("Products:\n");

            NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.US);
            for (Produit product : cartProducts) {
                int quantity = productQuantities.getOrDefault(product.getId(), 1);
                qrContent.append(" - ")
                        .append(product.getName())
                        .append(" x ").append(quantity)
                        .append(" = ").append(currencyFormat.format(product.getPrice() * quantity))
                        .append("\n");
            }

            LOGGER.info("Generating QR code for order: " + orderReference);
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/qr_code_view.fxml"));
            Parent root = loader.load();

            QRCodeViewController controller = loader.getController();
            controller.generateQRCode(qrContent.toString());

            Stage stage = new Stage();
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.setTitle("Purchase QR Code");
            stage.showAndWait();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to generate QR code", e);
            showAlert("Error", "Failed to generate QR code: " + e.getMessage());
        }
    }

    @FXML
    private void handleCancel() {
        try {
            LOGGER.info("Payment cancelled by user");
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/confirmation_purchase.fxml"));
            Parent root = loader.load();

            ConfirmationPurchaseController controller = loader.getController();
            controller.setCartProducts(cartProducts);
            controller.setProductQuantities(productQuantities);
            controller.setOrderReference(orderReference);
            controller.setTotalAmount(totalAmount);

            Stage stage = (Stage) totalAmountLabel.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Confirm Purchase");
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to navigate back", e);
            showAlert("Error", "Failed to navigate back: " + e.getMessage());
        }
    }

    private void navigateToProductView(boolean clearCart) {
        try {
            LOGGER.info("Navigating to product view, clearCart=" + clearCart);
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/product_client.fxml"));
            Parent root = loader.load();

            ProductFrontController controller = loader.getController();

            // Only pass cart products if we're not clearing the cart
            if (!clearCart) {
                controller.setCartProducts(cartProducts);
            }

            Stage stage = (Stage) totalAmountLabel.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("SahaTech Products");
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to navigate to products view", e);
            showAlert("Error", "Failed to navigate to products view: " + e.getMessage());
        }
    }

    private void showAlert(String title, String message) {
        LOGGER.info("Showing alert: " + title + " - " + message);
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}