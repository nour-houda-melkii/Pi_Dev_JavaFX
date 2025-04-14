package controllers;

import entities.Produit;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.text.NumberFormat;
import java.util.*;
import java.util.Properties;
import javax.mail.*;
import javax.mail.internet.*;

public class CartViewController {

    @FXML private TableView<Produit> cartTableView;
    @FXML private TableColumn<Produit, String> nameColumn;
    @FXML private TableColumn<Produit, Double> priceColumn;
    @FXML private TableColumn<Produit, Integer> quantityColumn;
    @FXML private TableColumn<Produit, Double> totalColumn;
    @FXML private TableColumn<Produit, String> actionColumn;
    @FXML private Label totalAmountLabel;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;

    private List<Produit> cartProducts;
    private Map<Integer, Integer> productQuantities = new HashMap<>();
    private String orderReference;
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.US);

    // Email configuration
    private static final String EMAIL_HOST = "smtp.gmail.com";
    private static final String EMAIL_PORT = "587";
    private static final String SENDER_EMAIL = "nourmelki05@gmail.com"; // Change to your store email
    private static final String SENDER_PASSWORD = "inom yuqm ciop jorf"; // Use app password for Gmail

    @FXML
    public void initialize() {
        configureTableColumns();
        // Generate a unique order reference on initialization
        orderReference = generateOrderReference();
    }

    private String generateOrderReference() {
        return "ORD-" + System.currentTimeMillis() + "-" + new Random().nextInt(1000);
    }

    private void configureTableColumns() {
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));

        priceColumn.setCellValueFactory(new PropertyValueFactory<>("price"));
        priceColumn.setCellFactory(column -> new TableCell<Produit, Double>() {
            @Override protected void updateItem(Double price, boolean empty) {
                super.updateItem(price, empty);
                setText(empty || price == null ? "" : currencyFormat.format(price));
            }
        });

        quantityColumn.setCellValueFactory(cellData -> {
            int productId = cellData.getValue().getId();
            int quantity = productQuantities.getOrDefault(productId, 1);
            return new SimpleIntegerProperty(quantity).asObject();
        });

        totalColumn.setCellValueFactory(cellData -> {
            Produit product = cellData.getValue();
            int quantity = productQuantities.getOrDefault(product.getId(), 1);
            return new SimpleDoubleProperty(product.getPrice() * quantity).asObject();
        });
        totalColumn.setCellFactory(column -> new TableCell<Produit, Double>() {
            @Override protected void updateItem(Double total, boolean empty) {
                super.updateItem(total, empty);
                setText(empty || total == null ? "" : currencyFormat.format(total));
            }
        });

        actionColumn.setCellValueFactory(cellData -> new SimpleStringProperty(""));
        actionColumn.setCellFactory(col -> new TableButtonCell());
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
    private void handleCheckout() {
        if (cartProducts.isEmpty()) {
            showAlert("Cart Empty", "Your cart is empty. Add some products first!");
            return;
        }

        // Check if email is valid
        if (emailField == null || emailField.getText().isEmpty() || !isValidEmail(emailField.getText())) {
            showAlert("Invalid Email", "Please enter a valid email address to continue.");
            return;
        }

        // Check if password is entered
        if (passwordField == null || passwordField.getText().isEmpty()) {
            showAlert("Missing Password", "Please enter your password to continue.");
            return;
        }

        // Send confirmation email
        boolean emailSent = sendConfirmationEmail(emailField.getText(), passwordField.getText());

        if (emailSent) {
            // Show confirmation dialog
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Order Confirmed");
            alert.setHeaderText("Thank you for your purchase!");
            alert.setContentText("Order Reference: " + orderReference + "\nTotal amount: " + totalAmountLabel.getText() +
                    "\nA confirmation email has been sent to " + emailField.getText());
            alert.showAndWait();

            // Show QR code
            showQRCode();

            // Clear the cart
            cartProducts.clear();
            cartTableView.getItems().clear();
            updateTotal();

            // Close this window
            ((Stage) cartTableView.getScene().getWindow()).close();
        } else {
            showAlert("Email Error", "Could not send confirmation email. Please try again.");
        }
    }

    private boolean isValidEmail(String email) {
        String emailRegex = "^[A-Za-z0-9+_.-]+@(.+)$";
        return email.matches(emailRegex);
    }

    private boolean sendConfirmationEmail(String toEmail, String password) {
        try {
            // Set up mail server properties
            Properties properties = new Properties();
            properties.put("mail.smtp.host", EMAIL_HOST);
            properties.put("mail.smtp.port", EMAIL_PORT);
            properties.put("mail.smtp.auth", "true");
            properties.put("mail.smtp.starttls.enable", "true");

            // Create authenticator with customer credentials
            Authenticator auth = new Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(SENDER_EMAIL, SENDER_PASSWORD);
                }
            };

            // Create mail session
            Session session = Session.getInstance(properties, auth);

            // Create the email message
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(SENDER_EMAIL));
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(toEmail));
            message.setSubject("SahaTech - Order Confirmation - " + orderReference);

            // Build email content with enhanced design and logo
            StringBuilder emailContent = new StringBuilder();
            emailContent.append("<!DOCTYPE html>");
            emailContent.append("<html><head>");
            emailContent.append("<style type='text/css'>");
            emailContent.append("body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; margin: 0; padding: 0; }");
            emailContent.append(".container { max-width: 600px; margin: 0 auto; padding: 20px; }");
            emailContent.append(".header { background-color: #3498db; padding: 20px; text-align: center; color: white; border-radius: 5px 5px 0 0; }");
            emailContent.append(".content { background-color: #f9f9f9; padding: 20px; border-left: 1px solid #ddd; border-right: 1px solid #ddd; }");
            emailContent.append(".footer { background-color: #3498db; color: white; text-align: center; padding: 15px; border-radius: 0 0 5px 5px; font-size: 12px; }");
            emailContent.append(".logo { width: 150px; height: auto; }");
            emailContent.append("table.order-details { width: 100%; border-collapse: collapse; margin: 20px 0; }");
            emailContent.append("table.order-details th { background-color: #3498db; color: white; text-align: left; padding: 10px; }");
            emailContent.append("table.order-details td { padding: 10px; border-bottom: 1px solid #ddd; }");
            emailContent.append(".total-row { font-weight: bold; background-color: #f5f5f5; }");
            emailContent.append(".order-info { background-color: white; border: 1px solid #ddd; border-radius: 5px; padding: 15px; margin-bottom: 20px; }");
            emailContent.append(".btn { display: inline-block; padding: 10px 20px; background-color: #3498db; color: white; text-decoration: none; border-radius: 5px; margin-top: 15px; }");
            emailContent.append("</style>");
            emailContent.append("</head><body>");
            emailContent.append("<div class='container'>");

            // Header with logo
            emailContent.append("<div class='header'>");
            // Include your logo as a base64 encoded image or host it somewhere and use the URL
            emailContent.append("<img src='https://your-domain.com/images/sahatech-logo.png' alt='SahaTech Logo' class='logo'>");
            emailContent.append("<h1>Order Confirmation</h1>");
            emailContent.append("</div>");

            // Main content
            emailContent.append("<div class='content'>");
            emailContent.append("<h2>Thank you for your purchase!</h2>");
            emailContent.append("<p>Dear Customer,</p>");
            emailContent.append("<p>Your order has been confirmed and is being processed. Below are your order details:</p>");

            // Order information box
            emailContent.append("<div class='order-info'>");
            emailContent.append("<p><strong>Order Reference:</strong> ").append(orderReference).append("</p>");
            emailContent.append("<p><strong>Order Date:</strong> ").append(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date())).append("</p>");
            emailContent.append("<p><strong>Email:</strong> ").append(toEmail).append("</p>");
            emailContent.append("</div>");

            // Order details table
            emailContent.append("<h3>Order Summary</h3>");
            emailContent.append("<table class='order-details'>");
            emailContent.append("<tr><th>Product</th><th>Quantity</th><th>Price</th><th>Total</th></tr>");

            double grandTotal = 0;
            for (Produit product : cartTableView.getItems()) {
                int quantity = productQuantities.getOrDefault(product.getId(), 1);
                double total = product.getPrice() * quantity;
                grandTotal += total;

                emailContent.append("<tr>");
                emailContent.append("<td>").append(product.getName()).append("</td>");
                emailContent.append("<td align='center'>").append(quantity).append("</td>");
                emailContent.append("<td align='right'>").append(currencyFormat.format(product.getPrice())).append("</td>");
                emailContent.append("<td align='right'>").append(currencyFormat.format(total)).append("</td>");
                emailContent.append("</tr>");
            }

            // Total row
            emailContent.append("<tr class='total-row'>");
            emailContent.append("<td colspan='3' align='right'><strong>Grand Total:</strong></td>");
            emailContent.append("<td align='right'><strong>").append(currencyFormat.format(grandTotal)).append("</strong></td>");
            emailContent.append("</tr>");
            emailContent.append("</table>");

            // Call to action button
            emailContent.append("<div style='text-align: center;'>");
            emailContent.append("<a href='https://sahatech.com/track-order?ref=").append(orderReference).append("' class='btn'>Track Your Order</a>");
            emailContent.append("</div>");

            emailContent.append("<p>If you have any questions or need assistance, please don't hesitate to contact our customer service at <a href='mailto:support@sahatech.com'>support@sahatech.com</a>.</p>");
            emailContent.append("</div>");

            // Footer
            emailContent.append("<div class='footer'>");
            emailContent.append("<p>© ").append(java.time.Year.now().toString()).append(" SahaTech. All rights reserved.</p>");
            emailContent.append("<p>123 Tech Street, Innovation City, Country</p>");
            emailContent.append("<div style='margin-top: 10px;'>");
            emailContent.append("<a href='https://facebook.com/sahatech' style='color: white; margin: 0 5px;'>Facebook</a> | ");
            emailContent.append("<a href='https://twitter.com/sahatech' style='color: white; margin: 0 5px;'>Twitter</a> | ");
            emailContent.append("<a href='https://instagram.com/sahatech' style='color: white; margin: 0 5px;'>Instagram</a>");
            emailContent.append("</div>");
            emailContent.append("</div>");

            emailContent.append("</div>");
            emailContent.append("</body></html>");

            // Set email content
            message.setContent(emailContent.toString(), "text/html");

            // Send the email
            Transport.send(message);

            System.out.println("Confirmation email sent successfully to: " + toEmail);
            return true;
        } catch (MessagingException e) {
            e.printStackTrace();
            System.err.println("Failed to send email: " + e.getMessage());
            return false;
        }
    }

    private void showQRCode() {
        try {
            StringBuilder qrContent = new StringBuilder();
            qrContent.append("Order: ").append(orderReference).append("\n");
            qrContent.append("Total: ").append(totalAmountLabel.getText()).append("\n");
            qrContent.append("Products:\n");

            for (Produit product : cartTableView.getItems()) {
                int quantity = productQuantities.getOrDefault(product.getId(), 1);
                qrContent.append(" - ")
                        .append(product.getName())
                        .append(" x ").append(quantity)
                        .append(" = ").append(currencyFormat.format(product.getPrice() * quantity))
                        .append("\n");
            }

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/qr_code_view.fxml"));
            Parent root = loader.load();

            QRCodeViewController controller = loader.getController();
            controller.generateQRCode(qrContent.toString());

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.setTitle("Purchase QR Code");
            stage.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to generate QR code: " + e.getMessage());
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
        cartTableView.setItems(FXCollections.observableArrayList(cartProducts));
        updateTotal();
    }

    private class TableButtonCell extends TableCell<Produit, String> {
        private final Button removeButton = new Button("Remove");

        TableButtonCell() {
            removeButton.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;");
            removeButton.setOnAction(event -> {
                Produit product = getTableView().getItems().get(getIndex());
                cartProducts.removeIf(p -> p.getId() == product.getId());
                getTableView().getItems().remove(product);
                updateTotal();
            });
        }

        @Override
        protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);
            setGraphic(empty ? null : removeButton);
        }
    }

    @FXML
    private void handleClearCart() {
        cartProducts.clear();
        productQuantities.clear();
        cartTableView.getItems().clear();
        updateTotal();
    }

    @FXML
    private void handleClose() {
        ((Stage) totalAmountLabel.getScene().getWindow()).close();
    }

    @FXML
    private void handleViewCart() {
        try {
            // Get the resource URL to verify it exists
            java.net.URL resourceUrl = getClass().getResource("/cart_view.fxml");

            if (resourceUrl == null) {
                showAlert("Error", "Could not find resource: /cart_view.fxml");
                return;
            }

            FXMLLoader loader = new FXMLLoader(resourceUrl);
            Parent root = loader.load();

            CartViewController controller = loader.getController();
            controller.setCartProducts(cartProducts);

            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("Shopping Cart");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to open cart view: " + e.getMessage() +
                    "\nResource URL: " + getClass().getResource("/cart_view.fxml"));
        }
    }
}