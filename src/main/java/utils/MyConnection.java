package utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MyConnection {
    private static final String URL = "jdbc:mysql://localhost:3306/pi_data_base1";
    private static final String USER = "root";
    private static final String PASSWORD = "";

    private static Connection connection;

    public static Connection getInstance() {
        try {
            if (connection == null || connection.isClosed()) {
                connection = DriverManager.getConnection(URL, USER, PASSWORD);
                System.out.println("✅ Connexion établie avec la base de données.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("❌ Échec de la connexion à la base de données.");
        }

        return connection;
    }
}
