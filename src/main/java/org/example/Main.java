package org.example;

public class Main {
    public static void main(String[] args) {
        // Ajouter les options VM nécessaires pour le fonctionnement de WebView
        System.setProperty("javafx.web.useWebKit", "true");
        
        // Lancer l'application JavaFX
        App.main(args);
    }
} 