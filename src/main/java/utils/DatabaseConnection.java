package utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {
    // Paramètres de connexion pour la base de données pi_data_base1
    private static final String URL = "jdbc:mysql://localhost:3306/pi_data_base1";
    private static final String USER = "root";
    private static final String PASSWORD = "";
    
    private static boolean warnedAboutDefaults = false;

    public static Connection getConnection() throws SQLException {
        // Vérifier si les paramètres sont ceux attendus
        if (!warnedAboutDefaults && 
            (!URL.contains("pi_data_base1") || 
             !USER.equals("root"))) {
            
            System.out.println("AVERTISSEMENT: Vous pourriez avoir besoin de configurer DatabaseConnection.java avec vos propres informations de connexion.");
            warnedAboutDefaults = true;
        }
        
        try {
            return DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (SQLException e) {
            System.err.println("Erreur de connexion à la base de données: " + e.getMessage());
            throw e;
        }
    }
} 