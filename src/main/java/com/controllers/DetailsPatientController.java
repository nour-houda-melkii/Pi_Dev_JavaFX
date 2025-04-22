package com.controllers;

import com.exceptions.AuthException;
import com.models.User;
import com.services.UserService;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.AbstractMap;


public class DetailsPatientController {
    @FXML private Label idField;
    @FXML private Label fullNameField;
    @FXML private Label emailField;
    @FXML private Label phoneField;
    @FXML private Label addressField;
    @FXML private Label ageField;
    @FXML private Label genderField;

    private UserService userService = new UserService();
    private User patient;

    public void initData(int patientId) {
        this.patient = userService.rechercherPatientParId(patientId);
        displayPatientData();
    }

    private void displayPatientData() {
        fullNameField.setText(patient.getFirstName() + " " + patient.getLastName());
        emailField.setText(patient.getEmail());
        phoneField.setText(patient.getPhoneNumber());
        addressField.setText(patient.getAddress());
        ageField.setText(String.valueOf(patient.getAge()));
        genderField.setText(patient.getGender().toString());
    }


    @FXML
    private void handleViewMedicalFile() {
        try {
            // Check if the patient has a medical file
            if (patient.getMedicalFile() == null || patient.getMedicalFile().isEmpty()) {
                showAlert("No Medical File", "Information",
                        "This patient does not have a medical record.");
                return; // Stop execution here
            }

            // Retrieve the medical file from the service
            AbstractMap.SimpleEntry<String, byte[]> fileEntry = userService.getMedicalFileContent(patient.getId());

            // Create a temporary file for display
            File tempFile = File.createTempFile("medical_", getFileExtension(fileEntry.getKey()));
            try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                fos.write(fileEntry.getValue());
            }

            // Open the file with the default application
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(tempFile);
            } else {
                showAlert("Information", "Visualization",
                        "The file has been retrieved but cannot be opened automatically. " +
                                "It is located at: " + tempFile.getAbsolutePath());
            }
        } catch (IOException e) {
            showAlert("Error", "File", "Error during visualization: " + e.getMessage());
        }
    }


    private String getFileExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf(".");
        if (lastDotIndex == -1) {
            return ""; // Pas d'extension
        }
        return fileName.substring(lastDotIndex);
    }


    @FXML
    private void handleBack() {
        Stage stage = (Stage) idField.getScene().getWindow();
        stage.close();
    }

    private void showAlert(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}