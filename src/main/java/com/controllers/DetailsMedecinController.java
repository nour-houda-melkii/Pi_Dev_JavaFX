package com.controllers;

import com.models.User;
import com.services.UserService;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class DetailsMedecinController {
    @FXML private Label titleLabel;
    @FXML private Label fullNameLabel;
    @FXML private Label emailLabel;
    @FXML private Label phoneLabel;
    @FXML private Label ageLabel;
    @FXML private Label genderLabel;
    @FXML private Label specialiteLabel;
    @FXML private Label licenceLabel;
    @FXML private Label addressLabel;

    private final UserService userService = new UserService();

    public void initData(int medecinId) {
        User medecin = userService.rechercherMedecinParId(medecinId);
        if (medecin != null) {
            titleLabel.setText("Doctor Details - " + medecin.getFirstName() + " " + medecin.getLastName());
            fullNameLabel.setText(medecin.getFirstName() + " " + medecin.getLastName());
            emailLabel.setText(medecin.getEmail());
            phoneLabel.setText(medecin.getPhoneNumber());
            ageLabel.setText(String.valueOf(medecin.getAge()));
            genderLabel.setText(medecin.getGender().toString());
            specialiteLabel.setText(medecin.getSpecialite() != null ? medecin.getSpecialite().toString() : "Non spécifiée");
            licenceLabel.setText(medecin.getNumeroLicence() != null ? medecin.getNumeroLicence() : "Non spécifié");
            addressLabel.setText(medecin.getAddress());
        }
    }

    @FXML
    private void handleClose() {
        ((Stage) titleLabel.getScene().getWindow()).close();
    }
}