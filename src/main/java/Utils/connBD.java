package utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class connBD {
    private static final String URL = "jdbc:mysql://localhost:3306/pireclamation";
    private static final String USER = "root";
    private static final String PASSWORD = "";

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("Erreur: Driver MySQL non trouvé!");
            e.printStackTrace();
            throw new ExceptionInInitializerError(e);
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    public static void testConnection() {
        try (Connection conn = getConnection()) {
            System.out.println("Connexion à la base de données réussie!");
        } catch (SQLException e) {
            System.err.println("Échec de la connexion à la base de données:");
            e.printStackTrace();
        }
    }
}