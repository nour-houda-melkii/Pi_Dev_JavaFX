package com.controllers.nour;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import com.models.*;
import com.services.*;
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
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
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

    // Stripe API key configuration
    // Around line 47 - Update visibility and implementation of the Stripe API keys
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
        // JavaScript is enabled by default in WebEngine

        // Enable WebView debugging
        System.setProperty("javafx.platform", "gtk");
        System.setProperty("prism.order", "sw");
        System.setProperty("prism.verbose", "true");

        // Add URL change listener for direct method call approach
        webEngine.locationProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && newValue.startsWith("java-callback:")) {
                handleJavaCallback(newValue);
            }
        });

        // Load HTML with Stripe Elements
        String stripeHtml = generateStripeHtml();
        webEngine.loadContent(stripeHtml);

        // Add JavaScript bridge to handle callbacks
        webEngine.getLoadWorker().stateProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.equals(Worker.State.SUCCEEDED)) {
                LOGGER.info("WebView loaded successfully");

                // Create and expose the JavaConnector object explicitly
                JavaConnector connector = new JavaConnector();
                JSObject window = (JSObject) webEngine.executeScript("window");
                window.setMember("javaConnector", connector);

                // Add console logging
                webEngine.executeScript(
                        "console.log = function(message) { " +
                                "  if (window.javaConnector) { " +
                                "    window.javaConnector.log(message); " +
                                "  } else { " +
                                "    alert('Console: ' + message);" +
                                "  }" +
                                "};"
                );

                // Test the connector
                webEngine.executeScript(
                        "try { " +
                                "  console.log('Testing Java connector: ' + (typeof window.javaConnector)); " +
                                "  if (window.javaConnector) {" +
                                "    console.log('Java connector methods: ' + " +
                                "      Object.getOwnPropertyNames(window.javaConnector).join(', ')); " +
                                "  }" +
                                "} catch(e) { " +
                                "  console.log('Java connector test failed: ' + e); " +
                                "}"
                );

                // Update cardholder name in JavaScript
                if (customerName != null && !customerName.isEmpty()) {
                    webEngine.executeScript("window.setCardholderName('" + customerName.replace("'", "\\'") + "');");
                    LOGGER.info("Set cardholder name in JS: " + customerName);
                }

                LOGGER.info("JavaScript bridge setup complete");
            }
        });
    }

    private void handleJavaCallback(String url) {
        try {
            LOGGER.info("Received Java callback URL: " + url);

            if (url.startsWith("java-callback:processPayment/")) {
                String paymentMethodId = URLDecoder.decode(
                        url.substring("java-callback:processPayment/".length()),
                        StandardCharsets.UTF_8
                );
                LOGGER.info("Extracted payment method ID from URL: " + paymentMethodId);
                startPaymentProcess(paymentMethodId);
            } else if (url.startsWith("java-callback:error/")) {
                String errorMessage = URLDecoder.decode(
                        url.substring("java-callback:error/".length()),
                        StandardCharsets.UTF_8
                );
                LOGGER.warning("Received error from JavaScript: " + errorMessage);
                Platform.runLater(() -> {
                    progressIndicator.setVisible(false);
                    paymentFormContainer.setDisable(false);
                    showAlert("Payment Error", errorMessage);
                });
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error handling Java callback", e);
        }
    }

    public class JavaConnector {
        public void processPayment(String paymentMethodId) {
            LOGGER.info("JavaConnector.processPayment called with ID: " + paymentMethodId);
            System.out.println("JavaConnector.processPayment called with ID: " + paymentMethodId);

            Platform.runLater(() -> {
                try {
                    LOGGER.info("Processing payment with method ID: " + paymentMethodId);
                    startPaymentProcess(paymentMethodId);
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Error in processPayment", e);
                    System.out.println("Error in processPayment: " + e.getMessage());
                    progressIndicator.setVisible(false);
                    paymentFormContainer.setDisable(false);
                    showAlert("Payment Error", "An error occurred: " + e.getMessage());
                }
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
                                    .setCountry("TN")
                                    .build())
                            .setPhone(customerPhone)
                            .build();
                    paramsBuilder.setShipping(shipping);
                }

                PaymentIntentCreateParams createParams = paramsBuilder.build();

                // Create and confirm the PaymentIntent
                PaymentIntent intent = PaymentIntent.create(createParams);

                LOGGER.info("Payment intent created: " + intent.getId() + ", status: " + intent.getStatus());

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

                // Update customer's order status in database if payment succeeded
                if (success) {
                    // You can add database operations here to update the order status
                    LOGGER.info("Payment successful, updating order status in database");

                    // Send updated email with payment confirmation
                    try {
                        sendPaymentConfirmationEmail();
                    } catch (Exception e) {
                        LOGGER.log(Level.WARNING, "Failed to send payment confirmation email", e);
                    }
                }

                return success;

            } catch (StripeException e) {
                LOGGER.log(Level.SEVERE, "Stripe payment processing error", e);
                Platform.runLater(() -> {
                    showAlert("Payment Processing Error", e.getMessage());
                });
                return false;
            }
        }).thenAccept(success -> {
            Platform.runLater(() -> {
                progressIndicator.setVisible(false);

                if (success) {
                    LOGGER.info("Payment successful");
                    showAlert("Payment Successful", "Your payment was processed successfully! Your order is now complete and will be shipped soon.");
                    // Navigate back to product view and clear the cart
                    navigateToProductView(true);
                } else {
                    LOGGER.warning("Payment processing issue");
                    showAlert("Payment Status", "There was an issue with your payment. Please try again or contact customer support.");
                    paymentFormContainer.setDisable(false);
                }
            });
        }).exceptionally(ex -> {
            LOGGER.log(Level.SEVERE, "Unexpected error during payment processing", ex);
            Platform.runLater(() -> {
                progressIndicator.setVisible(false);
                paymentFormContainer.setDisable(false);
                showAlert("Payment Processing Error", "An unexpected error occurred: " + ex.getMessage());
            });
            return null;
        });
    }

    private void sendPaymentConfirmationEmail() {
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
                "        body { font-family: 'Helvetica Neue', Helvetica, sans-serif; font-size: 16px; margin: 0; padding: 20px; }\n" +
                "        .container { max-width: 500px; margin: 0 auto; }\n" +
                "        #card-element { margin-bottom: 24px; padding: 12px; border: 1px solid #e6e6e6; border-radius: 4px; background: white; }\n" +
                "        button { background: #5469d4; color: #ffffff; font-family: Arial, sans-serif; border-radius: 4px; border: 0; padding: 12px 16px; font-size: 16px; font-weight: 600; cursor: pointer; display: block; transition: all 0.2s ease; box-shadow: 0px 4px 5.5px 0px rgba(0, 0, 0, 0.07); width: 100%; }\n" +
                "        button:hover { filter: contrast(115%); }\n" +
                "        button:disabled { opacity: 0.5; cursor: default; }\n" +
                "        .spinner { display: inline-block; position: relative; width: 20px; height: 20px; }\n" +
                "        .spinner:after { content: \" \"; display: block; width: 16px; height: 16px; margin: 2px; border-radius: 50%; border: 2px solid #ffffff; border-color: #ffffff transparent #ffffff transparent; animation: spinner 1.2s linear infinite; }\n" +
                "        @keyframes spinner { 0% { transform: rotate(0deg); } 100% { transform: rotate(360deg); } }\n" +
                "        #card-error { color: #dc3545; text-align: left; font-size: 13px; line-height: 17px; margin-top: 12px; }\n" +
                "        #debug-info { margin-top: 20px; font-size: 12px; color: #666; }\n" +
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
                "            <div id=\"debug-info\"></div>\n" +
                "        </form>\n" +
                "    </div>\n" +
                "    <script>\n" +
                "        // Debug info display function\n" +
                "        function debugLog(message) {\n" +
                "            console.log(message);\n" +
                "            document.getElementById('debug-info').innerHTML += '<div>' + message + '</div>';\n" +
                "        }\n" +
                "        \n" +
                "        // Initialize Stripe\n" +
                "        try {\n" +
                "            var stripe = Stripe('" + STRIPE_PUBLIC_KEY + "');\n" +
                "            debugLog('Stripe initialized successfully');\n" +
                "        } catch(e) {\n" +
                "            debugLog('Error initializing Stripe: ' + e.message);\n" +
                "            document.getElementById('card-error').textContent = 'Error initializing payment: ' + e.message;\n" +
                "        }\n" +
                "        \n" +
                "        // Create Elements instance\n" +
                "        try {\n" +
                "            var elements = stripe.elements();\n" +
                "            debugLog('Elements initialized');\n" +
                "        } catch(e) {\n" +
                "            debugLog('Error creating Elements: ' + e.message);\n" +
                "        }\n" +
                "        \n" +
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
                "        // Create and mount Card Element\n" +
                "        try {\n" +
                "            var card = elements.create('card', {\n" +
                "                style: style,\n" +
                "                hidePostalCode: false\n" +
                "            });\n" +
                "            card.mount('#card-element');\n" +
                "            debugLog('Card element mounted');\n" +
                "        } catch(e) {\n" +
                "            debugLog('Error mounting card element: ' + e.message);\n" +
                "        }\n" +
                "\n" +
                "        // Handle real-time validation errors from the Card Element\n" +
                "        card.on('change', function(event) {\n" +
                "            var displayError = document.getElementById('card-error');\n" +
                "            if (event.error) {\n" +
                "                displayError.textContent = event.error.message;\n" +
                "                debugLog('Card validation error: ' + event.error.message);\n" +
                "            } else {\n" +
                "                displayError.textContent = '';\n" +
                "            }\n" +
                "        });\n" +
                "\n" +
                "        // Test the Java connector after page loads\n" +
                "        window.addEventListener('load', function() {\n" +
                "            try {\n" +
                "                debugLog('Testing Java connector availability...');\n" +
                "                if (window.javaConnector) {\n" +
                "                    debugLog('Java connector found: ' + typeof window.javaConnector.processPayment);\n" +
                "                    if (typeof window.javaConnector.processPayment === 'function') {\n" +
                "                        debugLog('processPayment method is available');\n" +
                "                    } else {\n" +
                "                        debugLog('WARNING: processPayment method not found on javaConnector!');\n" +
                "                    }\n" +
                "                } else {\n" +
                "                    debugLog('WARNING: Java connector not found!');\n" +
                "                }\n" +
                "            } catch(e) {\n" +
                "                debugLog('Error testing Java connector: ' + e);\n" +
                "            }\n" +
                "        });\n" +
                "\n" +
                "        // Handle form submission\n" +
                "        var form = document.getElementById('payment-form');\n" +
                "        var submitButton = document.getElementById('submit-button');\n" +
                "        \n" +
                "        form.addEventListener('submit', function(ev) {\n" +
                "            ev.preventDefault();\n" +
                "            debugLog('Form submitted, processing payment...');\n" +
                "            \n" +
                "            // Disable the submit button to prevent repeated clicks\n" +
                "            submitButton.disabled = true;\n" +
                "            submitButton.innerHTML = '<div class=\"spinner\"></div><span id=\"button-text\">Processing...</span>';\n" +
                "            \n" +
                "            // Create payment method\n" +
                "            debugLog('Creating payment method...');\n" +
                "            stripe.createPaymentMethod({\n" +
                "                type: 'card',\n" +
                "                card: card,\n" +
                "                billing_details: {\n" +
                "                    name: window.cardholderName || ''\n" +
                "                }\n" +
                "            }).then(function(result) {\n" +
                "                if (result.error) {\n" +
                "                    // Show error in payment form\n" +
                "                    debugLog('Error creating payment method: ' + result.error.message);\n" +
                "                    var errorElement = document.getElementById('card-error');\n" +
                "                    errorElement.textContent = result.error.message;\n" +
                "                    submitButton.disabled = false;\n" +
                "                    submitButton.innerHTML = '<span id=\"button-text\">Pay " + totalAmount + "</span>';\n" +
                "                    \n" +
                "                    // Also notify Java about the error\n" +
                "                    try {\n" +
                "                        if (window.javaConnector && typeof window.javaConnector.handleError === 'function') {\n" +
                "                            window.javaConnector.handleError(result.error.message);\n" +
                "                            debugLog('Java connector notified about error');\n" +
                "                        } else {\n" +
                "                            // Alternative direct method call through URL\n" +
                "                            window.location.href = 'java-callback:error/' + encodeURIComponent(result.error.message);\n" +
                "                        }\n" +
                "                    } catch (e) {\n" +
                "                        debugLog('Failed to notify Java connector about error: ' + e);\n" +
                "                    }\n" +
                "                } else {\n" +
                "                    // Payment method created\n" +
                "                    debugLog('Payment method created: ' + result.paymentMethod.id);\n" +
                "                    \n" +
                "                    // Try using the Java connector first\n" +
                "                    var javaConnectorSuccess = false;\n" +
                "                    try {\n" +
                "                        debugLog('About to call Java connector...');\n" +
                "                        if (window.javaConnector && typeof window.javaConnector.processPayment === 'function') {\n" +
                "                            window.javaConnector.processPayment(result.paymentMethod.id);\n" +
                "                            javaConnectorSuccess = true;\n" +
                "                            debugLog('Java connector called successfully');\n" +
                "                        } else {\n" +
                "                            debugLog('Java connector or processPayment method not available, falling back to URL method');\n" +
                "                        }\n" +
                "                    } catch (e) {\n" +
                "                        debugLog('Error calling Java connector: ' + e);\n" +
                "                    }\n" +
                "                    \n" +
                "                    // If Java connector failed, try the URL method\n" +
                "                    if (!javaConnectorSuccess) {\n" +
                "                        try {\n" +
                "                            debugLog('Using URL callback as fallback');\n" +
                "                            window.location.href = 'java-callback:processPayment/' + encodeURIComponent(result.paymentMethod.id);\n" +
                "                        } catch (e) {\n" +
                "                            debugLog('Error using URL callback: ' + e);\n" +
                "                            document.getElementById('card-error').textContent = 'Error communicating with payment system: ' + e;\n" +
                "                            submitButton.disabled = false;\n" +
                "                            submitButton.innerHTML = '<span id=\"button-text\">Pay " + totalAmount + "</span>';\n" +
                "                        }\n" +
                "                    }\n" +
                "                }\n" +
                "            }).catch(function(e) {\n" +
                "                debugLog('Exception in payment processing: ' + e);\n" +
                "                document.getElementById('card-error').textContent = 'An unexpected error occurred: ' + e;\n" +
                "                submitButton.disabled = false;\n" +
                "                submitButton.innerHTML = '<span id=\"button-text\">Pay " + totalAmount + "</span>';\n" +
                "            });\n" +
                "        });\n" +
                "        \n" +
                "        // Connect cardholder name from Java\n" +
                "        window.setCardholderName = function(name) {\n" +
                "            window.cardholderName = name;\n" +
                "            debugLog('Cardholder name set to: ' + name);\n" +
                "        };\n" +
                "        \n" +
                "        debugLog('Stripe payment form initialization complete');\n" +
                "    </script>\n" +
                "</body>\n" +
                "</html>";
    }

    @FXML
    private void handleCancel() {
        try {
            LOGGER.info("Payment cancelled by user");
            navigateToProductView(false); // Don't clear the cart
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to navigate back", e);
            showAlert("Error", "Failed to navigate back: " + e.getMessage());
        }
    }

    private void navigateToProductView(boolean clearCart) {
        try {
            LOGGER.info("Navigating to product view, clearCart=" + clearCart);
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/views/nour/product_client.fxml"));
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

    private static void showAlert(String title, String message) {
        LOGGER.info("Showing alert: " + title + " - " + message);
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    

}
