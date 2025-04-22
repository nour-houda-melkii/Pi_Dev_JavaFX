package com.tests;

import com.controllers.LoginController;
import com.services.AuthService;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainFX extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Load the FXML file
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/views/Login.fxml"));
        Parent root = loader.load();

        // Configure the controller
        LoginController controller = loader.getController();
        // Si votre LoginController a besoin d'un AuthService
        controller.setAuthService(new AuthService());

        // Configure the scene
        Scene scene = new Scene(root, 1000, 700);

        // Load CSS
        scene.getStylesheets().add(getClass().getResource("/styles/styleLogin/login.css").toExternalForm());

        // Configure the window
        primaryStage.setTitle("SAHATECK - Login");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(800);
        primaryStage.setMinHeight(600);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}