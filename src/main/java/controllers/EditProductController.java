package controllers;

import Services.AIDescriptionService;
import Services.CategoryServices;
import Services.ProduitServices;
import entities.Category;
import entities.Produit;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.util.List;

public class EditProductController {

    @FXML
    private TextField nameField;

    @FXML
    private TextField priceField;

    @FXML
    private TextField quantityField;

    @FXML
    private TextField descriptionField;

    @FXML
    private TextField imageField;

    @FXML
    private ComboBox<Category> categoryCombo;

    @FXML
    private ImageView imagePreview;

    @FXML
    private Button generateDescriptionButton;

    private ProduitServices produitService = new ProduitServices();
    private final CategoryServices categoryService = new CategoryServices();
    private final AIDescriptionService aiService = new AIDescriptionService();

    private Produit productToEdit = null;

    public void initialize() {
        try {
            loadCategories();

            // Add listener to name field to enable generate button only when name is entered
            nameField.textProperty().addListener((observable, oldValue, newValue) -> {
                generateDescriptionButton.setDisable(newValue == null || newValue.trim().isEmpty());
            });

            // Initially disable the generate button
            generateDescriptionButton.setDisable(true);

        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to load categories: " + e.getMessage());
        }
    }

    public void setProductToEdit(Produit product) {
        this.productToEdit = product;
        if (product != null) {
            populateFields(product);
        }
    }

    private void populateFields(Produit produit) {
        nameField.setText(produit.getName());
        descriptionField.setText(produit.getDescription());
        priceField.setText(String.valueOf(produit.getPrice()));
        quantityField.setText(String.valueOf(produit.getQuantity()));
        imageField.setText(produit.getImagePath());

        // Load the image preview
        loadImagePreview(produit.getImagePath());

        // Set the category in the combo box
        try {
            for (Category category : categoryCombo.getItems()) {
                if (category.getId() == produit.getCategoryId()) {
                    categoryCombo.setValue(category);
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadCategories() throws SQLException {
        List<Category> categories = categoryService.showAll();
        categoryCombo.setItems(FXCollections.observableArrayList(categories));

        // Set up a cell factory to display category names instead of toString()
        categoryCombo.setCellFactory(param -> new ListCell<Category>() {
            @Override
            protected void updateItem(Category category, boolean empty) {
                super.updateItem(category, empty);
                if (empty || category == null) {
                    setText(null);
                } else {
                    setText(category.getName());
                }
            }
        });

        // Same for the displayed value
        categoryCombo.setButtonCell(new ListCell<Category>() {
            @Override
            protected void updateItem(Category category, boolean empty) {
                super.updateItem(category, empty);
                if (empty || category == null) {
                    setText(null);
                } else {
                    setText(category.getName());
                }
            }
        });
    }

    private void loadImagePreview(String imagePath) {
        try {
            if (imagePath == null || imagePath.isEmpty()) {
                setPlaceholderImage();
                return;
            }

            Image image;
            if (imagePath.startsWith("/")) {
                image = new Image(getClass().getResourceAsStream(imagePath));
            } else if (imagePath.startsWith("file:")) {
                image = new Image(imagePath);
            } else {
                try {
                    image = new Image(getClass().getResourceAsStream("/images/" + imagePath));
                } catch (Exception e) {
                    image = new Image(new File(imagePath).toURI().toString());
                }
            }

            imagePreview.setImage(image);
        } catch (Exception e) {
            System.err.println("Error loading image preview: " + e.getMessage());
            e.printStackTrace();
            setPlaceholderImage();
        }
    }

    private void setPlaceholderImage() {
        try {
            Image placeholder = new Image(getClass().getResourceAsStream("/images/placeholder.png"));
            imagePreview.setImage(placeholder);
        } catch (Exception e) {
            imagePreview.setImage(null);
        }
    }

    @FXML
    void choisirImage(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg")
        );

        File selectedFile = fileChooser.showOpenDialog(null);
        if (selectedFile != null) {
            try {
                // Create the target directory if it doesn't exist
                File targetDir = new File("src/main/resources/images");
                if (!targetDir.exists()) {
                    targetDir.mkdirs();
                }

                // Copy the file to the target directory
                File destFile = new File(targetDir, selectedFile.getName());
                Files.copy(selectedFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

                // Set the relative path in the image field
                String imagePath = "src/main/resources/images/" + selectedFile.getName();
                imageField.setText(imagePath);

                // Update the image preview
                loadImagePreview(imagePath);

            } catch (IOException e) {
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to copy image: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    @FXML
    void generateAIDescription() {
        String productName = nameField.getText().trim();
        if (!productName.isEmpty()) {
            // Show loading indicator
            descriptionField.setText("Generating description...");

            // In a real application, you would want to do this in a background thread
            // to avoid freezing the UI during API calls
            new Thread(() -> {
                String generatedDescription = aiService.generateProductDescription(productName);

                // Update UI on the JavaFX application thread
                javafx.application.Platform.runLater(() -> {
                    descriptionField.setText(generatedDescription);
                });
            }).start();
        }
    }

    @FXML
    void updateProduct(ActionEvent event) {
        if (!validateFields()) return;

        try {
            // Check if we have a product to edit
            if (productToEdit == null) {
                showAlert(Alert.AlertType.ERROR, "Error", "No product selected for editing!");
                return;
            }

            // Update the product object with form values
            productToEdit.setName(nameField.getText());
            productToEdit.setDescription(descriptionField.getText());
            productToEdit.setPrice(Double.parseDouble(priceField.getText()));
            productToEdit.setQuantity(Integer.parseInt(quantityField.getText()));
            productToEdit.setImagePath(imageField.getText());
            productToEdit.setCategoryId(categoryCombo.getValue().getId());

            // Update the product in the database
            produitService.update(productToEdit);

            showSuccessAlert("Success", "Product updated successfully!");

            // Go back to the admin dashboard
            navigateToAdminDashboard();

        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to update product: " + e.getMessage());
        }
    }

    @FXML
    void cancelEdit(ActionEvent event) {
        // Go back to the admin dashboard without saving
        navigateToAdminDashboard();
    }

    private void navigateToAdminDashboard() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/admin_dashboard.fxml"));
            Stage stage = (Stage) nameField.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Admin Dashboard");
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Navigation Error", "Failed to navigate back: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private boolean validateFields() {
        if (nameField.getText().isEmpty() || descriptionField.getText().isEmpty() ||
                priceField.getText().isEmpty() || quantityField.getText().isEmpty() ||
                imageField.getText().isEmpty() || categoryCombo.getValue() == null) {

            showAlert(Alert.AlertType.WARNING, "Warning", "All fields must be filled!");
            return false;
        }

        // Check product name length - minimum 5 characters
        if (nameField.getText().trim().length() < 5) {
            showAlert(Alert.AlertType.ERROR, "Validation Error", "Product name must be at least 5 characters long!");
            return false;
        }

        try {
            double price = Double.parseDouble(priceField.getText());
            if (price < 0) {
                showAlert(Alert.AlertType.ERROR, "Error", "Price must be positive!");
                return false;
            }
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Price must be a valid number!");
            return false;
        }

        try {
            int quantity = Integer.parseInt(quantityField.getText());
            if (quantity < 0) {
                showAlert(Alert.AlertType.ERROR, "Error", "Quantity must be positive!");
                return false;
            }
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Quantity must be a valid integer!");
            return false;
        }

        return true;
    }

    private void showSuccessAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}