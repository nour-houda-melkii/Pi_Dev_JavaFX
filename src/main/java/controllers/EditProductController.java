package controllers;

import Services.AIDescriptionService;
import Services.CategoryServices;
import Services.DataPersistenceService;
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
    private DataPersistenceService dataPersistence = new DataPersistenceService();


    public void initialize() {
        try {
            loadCategories();

            nameField.textProperty().addListener((observable, oldValue, newValue) -> {
                generateDescriptionButton.setDisable(newValue == null || newValue.trim().isEmpty());
            });

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

        loadImagePreview(produit.getImagePath());

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
                File targetDir = new File("src/main/resources/images");
                if (!targetDir.exists()) {
                    targetDir.mkdirs();
                }

                File destFile = new File(targetDir, selectedFile.getName());
                Files.copy(selectedFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

                String imagePath = "src/main/resources/images/" + selectedFile.getName();
                imageField.setText(imagePath);

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
            descriptionField.setText("Generating description...");


            new Thread(() -> {
                String generatedDescription = aiService.generateProductDescription(productName);

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
            if (productToEdit == null) {
                showAlert(Alert.AlertType.ERROR, "Error", "No product selected for editing!");
                return;
            }

            // Get the current state before updating
            Produit oldProduct = produitService.getOne(productToEdit.getId());

            // Update the product fields
            productToEdit.setName(nameField.getText());
            productToEdit.setDescription(descriptionField.getText());
            productToEdit.setPrice(Double.parseDouble(priceField.getText()));
            productToEdit.setQuantity(Integer.parseInt(quantityField.getText()));
            productToEdit.setImagePath(imageField.getText());
            productToEdit.setCategoryId(categoryCombo.getValue().getId());

            // Update the product in the database
            produitService.update(productToEdit);

            // Record the update in history
            dataPersistence.recordProductUpdated(oldProduct, productToEdit);

            showSuccessAlert("Success", "Product updated successfully!");
            navigateToAdminDashboard();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to update product: " + e.getMessage());
        }
    }

    @FXML
    void cancelEdit(ActionEvent event) {
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