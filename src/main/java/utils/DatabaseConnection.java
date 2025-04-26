package utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

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
            // Configurer les propriétés de connexion pour accepter les dates zéro
            Properties connectionProps = new Properties();
            connectionProps.setProperty("user", USER);
            connectionProps.setProperty("password", PASSWORD);
            
            // Ces paramètres sont importants pour permettre les dates zéro
            connectionProps.setProperty("zeroDateTimeBehavior", "convertToNull");
            connectionProps.setProperty("allowPublicKeyRetrieval", "true");
            connectionProps.setProperty("useSSL", "false");
            
            return DriverManager.getConnection(URL, connectionProps);
        } catch (SQLException e) {
            System.err.println("Erreur de connexion à la base de données: " + e.getMessage());
            
            // Essayer sans les paramètres spéciaux en cas d'échec
            try {
                return DriverManager.getConnection(URL, USER, PASSWORD);
            } catch (SQLException e2) {
                System.err.println("Deuxième tentative échouée: " + e2.getMessage());
                throw e2;
            }
        }
    }
} 