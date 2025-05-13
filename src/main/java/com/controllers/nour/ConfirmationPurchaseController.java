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
import javafx.stage.Modality;
import javafx.stage.Stage;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.IOException;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class ConfirmationPurchaseController {

    private static final Logger LOGGER = Logger.getLogger(ConfirmationPurchaseController.class.getName());

    // Hardcoded customer information
    private static final String CUSTOMER_EMAIL = "nourmelki05@gmail.com";
    private static final String CUSTOMER_NAME = "Nour Melki";
    private static final String CUSTOMER_ADDRESS = "Ben Arous";
    private static final String CUSTOMER_PHONE = "27052401";

    @FXML private Label orderReferenceLabel;
    @FXML private Label totalAmountLabel;
    @FXML private Label itemCountLabel;
    @FXML private Button confirmButton;
    @FXML private Button paymentButton;

    private List<Produit> cartProducts;
    private Map<Integer, Integer> productQuantities = new HashMap<>();
    private String orderReference;
    private String totalAmount;
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.US);
    private boolean orderProcessed = false;

    // Email configuration
    private static final String EMAIL_HOST = "smtp.gmail.com";
    private static final String EMAIL_PORT = "587";
    private static final String SENDER_EMAIL = "nourmelki05@gmail.com";
    private static final String SENDER_PASSWORD = "inom yuqm ciop jorf";
    private int currentUserId;

    // Payment service
    private PaymentService paymentService;

    public ConfirmationPurchaseController() {
        this.paymentService = new PaymentService();
    }

    public void setCurrentUserId(int userId) {
        this.currentUserId = userId;
    }

    @FXML
    public void initialize() {
        // Initially hide the payment button until order is processed
        if (paymentButton != null) {
            paymentButton.setVisible(false);
            paymentButton.setManaged(false);
        }
    }

    public void setCartProducts(List<Produit> cartProducts) {
        this.cartProducts = cartProducts;
        updateItemCount();
    }

    public void setProductQuantities(Map<Integer, Integer> productQuantities) {
        this.productQuantities = productQuantities;
    }

    public void setOrderReference(String orderReference) {
        this.orderReference = orderReference;
        if (orderReferenceLabel != null) {
            this.orderReferenceLabel.setText(orderReference);
        }
    }

    public void setTotalAmount(String totalAmount) {
        this.totalAmount = totalAmount;
        if (totalAmountLabel != null) {
            this.totalAmountLabel.setText(totalAmount);
        }
    }

    private void updateItemCount() {
        int totalItems = 0;
        for (Produit product : cartProducts) {
            totalItems += productQuantities.getOrDefault(product.getId(), 1);
        }
        if (itemCountLabel != null) {
            itemCountLabel.setText(String.valueOf(totalItems));
        }
    }

    @FXML
    private void handleConfirmPurchase() {
        processOrder();
    }

    private void processOrder() {
        try {
            createOrderInDatabase();

            boolean emailSent = sendConfirmationEmail(
                    orderReference,
                    totalAmount,
                    cartProducts,
                    productQuantities
            );

            if (emailSent) {
                LOGGER.info("Confirmation email sent successfully to: " + CUSTOMER_EMAIL);
            } else {
                LOGGER.warning("Failed to send confirmation email to: " + CUSTOMER_EMAIL);
                showAlert("Email Notification", "Failed to send confirmation email.");
            }

            showQRCode();

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Order Confirmed");
            alert.setHeaderText("Thank you for your purchase!");
            alert.setContentText("Your order has been confirmed.\n\n" +
                    "Order Reference: " + orderReference + "\n" +
                    "Total amount: " + totalAmount + "\n\n" +
                    (emailSent ? "A confirmation email has been sent." :
                            "Failed to send confirmation email. Please contact support.") + "\n\n" +
                    "You will now be redirected to the payment page.");
            alert.showAndWait();

            orderProcessed = true;

            if (paymentButton != null) {
                paymentButton.setVisible(true);
                paymentButton.setManaged(true);
            }

            proceedToStripePayment();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to create order", e);
            showAlert("Order Error", "Failed to create order: " + e.getMessage());
        }
    }

    private void createOrderInDatabase() throws SQLException {
        double total = 0;
        for (Produit product : cartProducts) {
            int quantity = productQuantities.getOrDefault(product.getId(), 1);
            total += product.getPrice() * quantity;
        }

        Commande commande = new Commande();
        commande.setUserId(currentUserId);
        commande.setDateCommande(LocalDate.now());
        commande.setStatut("Pending Payment");
        commande.setTotal(total);

        List<CommandeLigne> lignes = cartProducts.stream()
                .map(product -> {
                    CommandeLigne ligne = new CommandeLigne();
                    ligne.setProduitId(product.getId());
                    ligne.setQuantity(productQuantities.getOrDefault(product.getId(), 1));
                    return ligne;
                })
                .collect(Collectors.toList());
    }

    @FXML
    private void proceedToStripePayment() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/views/nour/stripe_payment_view.fxml"));
            Parent root = loader.load();

            StripePaymentController controller = loader.getController();
            controller.setPaymentInfo(
                    cartProducts,
                    productQuantities,
                    orderReference,
                    totalAmount,
                    CUSTOMER_EMAIL,
                    CUSTOMER_NAME,
                    CUSTOMER_ADDRESS,
                    CUSTOMER_PHONE,
                    "" // No password needed
            );

            Stage stage = (Stage) orderReferenceLabel.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Stripe Payment");
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to navigate to Stripe payment view", e);
            showAlert("Error", "Failed to navigate to payment page: " + e.getMessage());
        }
    }

    private boolean sendConfirmationEmail(String orderRef, String amount,
                                          List<Produit> products, Map<Integer, Integer> quantities) {
        try {
            Properties properties = new Properties();
            properties.put("mail.smtp.host", EMAIL_HOST);
            properties.put("mail.smtp.port", EMAIL_PORT);
            properties.put("mail.smtp.auth", "true");
            properties.put("mail.smtp.starttls.enable", "true");

            Authenticator auth = new Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(SENDER_EMAIL, SENDER_PASSWORD);
                }
            };

            Session session = Session.getInstance(properties, auth);

            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(SENDER_EMAIL));
            message.addRecipient(javax.mail.Message.RecipientType.TO, new InternetAddress(CUSTOMER_EMAIL));            message.setSubject("SahaTech - Order Confirmation - " + orderRef);

            StringBuilder emailContent = new StringBuilder();
            emailContent.append("<!DOCTYPE html>");
            emailContent.append("<html><head>");
            emailContent.append("<style type='text/css'>");
            emailContent.append("body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; margin: 0; padding: 0; }");
            emailContent.append(".container { max-width: 600px; margin: 0 auto; padding: 20px; }");
            emailContent.append(".header { background-color: #33ccff; padding: 20px; text-align: center; color: white; border-radius: 5px 5px 0 0; }");
            emailContent.append(".content { background-color: #f9f9f9; padding: 20px; border-left: 1px solid #ddd; border-right: 1px solid #ddd; }");
            emailContent.append(".footer { background-color: #33ccff; color: white; text-align: center; padding: 15px; border-radius: 0 0 5px 5px; font-size: 12px; }");
            emailContent.append("table.order-details { width: 100%; border-collapse: collapse; margin: 20px 0; }");
            emailContent.append("table.order-details th { background-color: #33ccff; color: white; text-align: left; padding: 10px; }");
            emailContent.append("table.order-details td { padding: 10px; border-bottom: 1px solid #ddd; }");
            emailContent.append(".total-row { font-weight: bold; background-color: #f5f5f5; }");
            emailContent.append(".order-info { background-color: white; border: 1px solid #ddd; border-radius: 5px; padding: 15px; margin-bottom: 20px; }");
            emailContent.append(".btn { display: inline-block; padding: 10px 20px; background-color: #33ccff; color: white; text-decoration: none; border-radius: 5px; margin-top: 15px; }");
            emailContent.append(".payment-info { background-color: #e9f7ff; border-left: 4px solid #33ccff; padding: 10px; margin: 15px 0; }");
            emailContent.append("</style>");
            emailContent.append("</head><body>");
            emailContent.append("<div class='container'>");
            emailContent.append("<div class='header'>");
            emailContent.append("<h1>SAHATECH Order Confirmation</h1>");
            emailContent.append("</div>");
            emailContent.append("<div class='content'>");
            emailContent.append("<h2>Thank you for your purchase!</h2>");
            emailContent.append("<p>Dear ").append(CUSTOMER_NAME).append(",</p>");
            emailContent.append("<p>Your order has been confirmed and is being processed. Below are your order details:</p>");
            emailContent.append("<div class='payment-info'>");
            emailContent.append("<p><strong>Payment Status:</strong> Pending</p>");
            emailContent.append("<p><strong>Payment Method:</strong> Stripe (Online Payment)</p>");
            emailContent.append("</div>");
            emailContent.append("<div class='order-info'>");
            emailContent.append("<p><strong>Order Reference:</strong> ").append(orderRef).append("</p>");
            emailContent.append("<p><strong>Order Date:</strong> ").append(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date())).append("</p>");
            emailContent.append("<p><strong>Customer:</strong> ").append(CUSTOMER_NAME).append("</p>");
            emailContent.append("<p><strong>Email:</strong> ").append(CUSTOMER_EMAIL).append("</p>");
            emailContent.append("<p><strong>Shipping Address:</strong> ").append(CUSTOMER_ADDRESS).append("</p>");
            emailContent.append("<p><strong>Phone:</strong> ").append(CUSTOMER_PHONE).append("</p>");
            emailContent.append("</div>");
            emailContent.append("<h3>Order Summary</h3>");
            emailContent.append("<table class='order-details'>");
            emailContent.append("<tr><th>Product</th><th>Quantity</th><th>Price</th><th>Total</th></tr>");

            double grandTotal = 0;
            for (Produit product : products) {
                int quantity = quantities.getOrDefault(product.getId(), 1);
                double total = product.getPrice() * quantity;
                grandTotal += total;

                emailContent.append("<tr>");
                emailContent.append("<td>").append(product.getName()).append("</td>");
                emailContent.append("<td align='center'>").append(quantity).append("</td>");
                emailContent.append("<td align='right'>").append(currencyFormat.format(product.getPrice())).append("</td>");
                emailContent.append("<td align='right'>").append(currencyFormat.format(total)).append("</td>");
                emailContent.append("</tr>");
            }

            emailContent.append("<tr class='total-row'>");
            emailContent.append("<td colspan='3' align='right'><strong>Grand Total:</strong></td>");
            emailContent.append("<td align='right'><strong>").append(amount).append("</strong></td>");
            emailContent.append("</tr>");
            emailContent.append("</table>");
            emailContent.append("<p><strong>Payment Instructions:</strong> You'll be redirected to our secure payment page to complete your purchase.</p>");
            emailContent.append("<div style='text-align: center;'>");
            emailContent.append("<a href='https://sahatech.com/payment?ref=").append(orderRef).append("' class='btn'>Complete Payment</a>");
            emailContent.append("</div>");
            emailContent.append("<p>If you have any questions, please contact our customer service.</p>");
            emailContent.append("</div>");
            emailContent.append("<div class='footer'>");
            emailContent.append("<p>© ").append(java.time.Year.now().toString()).append(" SahaTech. All rights reserved.</p>");
            emailContent.append("</div>");
            emailContent.append("</div>");
            emailContent.append("</body></html>");

            message.setContent(emailContent.toString(), "text/html");
            Transport.send(message);

            return true;
        } catch (MessagingException e) {
            LOGGER.log(Level.SEVERE, "Failed to send email", e);
            return false;
        }
    }

    private void showQRCode() {
        try {
            StringBuilder qrContent = new StringBuilder();
            qrContent.append("Order: ").append(orderReference).append("\n");
            qrContent.append("Customer: ").append(CUSTOMER_NAME).append("\n");
            qrContent.append("Total: ").append(totalAmount).append("\n");
            qrContent.append("Products:\n");

            for (Produit product : cartProducts) {
                int quantity = productQuantities.getOrDefault(product.getId(), 1);
                qrContent.append(" - ")
                        .append(product.getName())
                        .append(" x ").append(quantity)
                        .append(" = ").append(currencyFormat.format(product.getPrice() * quantity))
                        .append("\n");
            }

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/views/nour/qr_code_view.fxml"));
            Parent root = loader.load();

            QRCodeViewController controller = loader.getController();
            controller.generateQRCode(qrContent.toString());

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.setTitle("Purchase QR Code");
            stage.showAndWait();

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to generate QR code", e);
            showAlert("Error", "Failed to generate QR code: " + e.getMessage());
        }
    }

    @FXML
    private void handleBackToCart() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/views/nour/cart_view.fxml"));
            Parent root = loader.load();

            CartViewController controller = loader.getController();
            controller.setCartProducts(cartProducts);

            Stage stage = (Stage) orderReferenceLabel.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Shopping Cart");
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to navigate back to cart view", e);
            showAlert("Error", "Failed to navigate back to cart view: " + e.getMessage());
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public void setCommandeId(int activeCommandeId) {
    }
}