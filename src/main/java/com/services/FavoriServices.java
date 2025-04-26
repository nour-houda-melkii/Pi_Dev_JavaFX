package com.services;

import com.utils.DataSource;
import com.models.*;

import java.sql.*;
import java.util.*;

public class FavoriServices {
    private Connection connection;

    public FavoriServices() {
        connection = DataSource.getInstance().getConnection();
    }

    // Add a product to favorites
    public void add(Favori favori) throws SQLException {
        // First check if this product is already in favorites for this user
        String checkQuery = "SELECT * FROM favori WHERE user_id = ? AND produit_id = ?";
        PreparedStatement checkStmt = connection.prepareStatement(checkQuery);
        checkStmt.setInt(1, favori.getUserId());
        checkStmt.setInt(2, favori.getProduitId());
        ResultSet checkResult = checkStmt.executeQuery();

        // If already exists, don't add it again
        if (checkResult.next()) {
            return;
        }

        String query = "INSERT INTO favori (user_id, produit_id) VALUES (?, ?)";
        PreparedStatement ps = connection.prepareStatement(query);
        ps.setInt(1, favori.getUserId());
        ps.setInt(2, favori.getProduitId());
        ps.executeUpdate();
    }

    // Remove a product from favorites
    public void delete(Favori favori) throws SQLException {
        String query = "DELETE FROM favori WHERE user_id = ? AND produit_id = ?";
        PreparedStatement ps = connection.prepareStatement(query);
        ps.setInt(1, favori.getUserId());
        ps.setInt(2, favori.getProduitId());
        ps.executeUpdate();
    }

    // Get all favorites for a specific user
    public List<Favori> getFavorisByUser(int userId) throws SQLException {
        List<Favori> favoris = new ArrayList<>();
        String query = "SELECT * FROM favori WHERE user_id = ?";
        PreparedStatement ps = connection.prepareStatement(query);
        ps.setInt(1, userId);
        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            Favori favori = new Favori();
            favori.setId(rs.getInt("id"));
            favori.setUserId(rs.getInt("user_id"));
            favori.setProduitId(rs.getInt("produit_id"));
            favoris.add(favori);
        }

        return favoris;
    }

    // Get products that are in a user's favorites (with discount applied)
    public List<Produit> getFavoriteProducts(int userId) throws SQLException {
        List<Produit> products = new ArrayList<>();
        String query = "SELECT p.* FROM produit p " +
                "JOIN favori f ON p.id = f.produit_id " +
                "WHERE f.user_id = ?";

        PreparedStatement ps = connection.prepareStatement(query);
        ps.setInt(1, userId);
        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            Produit produit = new Produit();
            produit.setId(rs.getInt("id"));
            produit.setName(rs.getString("name"));
            produit.setDescription(rs.getString("desciption"));

            // Apply 5% discount to the original price
            double originalPrice = rs.getDouble("price");
            double discountedPrice = originalPrice * 0.95; // 5% discount
            produit.setPrice(discountedPrice);

            produit.setImagePath(rs.getString("image"));
            produit.setQuantity(rs.getInt("quantity"));
            produit.setCategoryId(rs.getInt("category_id"));
            products.add(produit);
        }

        return products;
    }

    // Check if a product is in user's favorites
    public boolean isProductInFavorites(int userId, int productId) throws SQLException {
        String query = "SELECT * FROM favori WHERE user_id = ? AND produit_id = ?";
        PreparedStatement ps = connection.prepareStatement(query);
        ps.setInt(1, userId);
        ps.setInt(2, productId);
        ResultSet rs = ps.executeQuery();
        return rs.next(); // Returns true if there's at least one result
    }

    // Get statistics - most favorited products
    public Map<Produit, Integer> getMostFavoritedProducts() throws SQLException {
        Map<Produit, Integer> productCountMap = new LinkedHashMap<>();

        String query = "SELECT p.*, COUNT(f.id) as favorite_count " +
                "FROM produit p " +
                "JOIN favori f ON p.id = f.produit_id " +
                "GROUP BY p.id " +
                "ORDER BY favorite_count DESC";

        Statement stmt = connection.createStatement();
        ResultSet rs = stmt.executeQuery(query);

        ProduitServices produitServices = new ProduitServices();

        while (rs.next()) {
            Produit produit = produitServices.getOne(rs.getInt("id"));
            int count = rs.getInt("favorite_count");
            productCountMap.put(produit, count);
        }

        return productCountMap;
    }
}