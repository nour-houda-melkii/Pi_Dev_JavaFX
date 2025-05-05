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
    public boolean delete(Favori favori) throws SQLException {
        String query = "DELETE FROM favori WHERE user_id = ? AND produit_id = ?";
        PreparedStatement ps = connection.prepareStatement(query);
        ps.setInt(1, favori.getUserId());
        ps.setInt(2, favori.getProduitId());
        int result = ps.executeUpdate();
        return result > 0;
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

    // Get products that are in a user's favorites
    public List<Produit> getFavoriteProducts(int userId) throws SQLException {
        List<Produit> products = new ArrayList<>();

        // Get all products that are favorited by the user
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
            produit.setDesciption(rs.getString("desciption"));
            produit.setPrice(rs.getDouble("price"));
            produit.setImagePath(rs.getString("image"));
            produit.setQuantity(rs.getInt("quantity"));
            produit.setCategoryId(rs.getInt("category_id"));
            products.add(produit);
        }

        return products;
    }

    // Get products with no favorites at all
    public List<Produit> getProductsWithNoFavorites() throws SQLException {
        List<Produit> products = new ArrayList<>();

        // Get products with no favorites
        String query = "SELECT p.* FROM produit p " +
                "LEFT JOIN favori f ON p.id = f.produit_id " +
                "WHERE f.id IS NULL";

        Statement stmt = connection.createStatement();
        ResultSet rs = stmt.executeQuery(query);

        while (rs.next()) {
            Produit produit = new Produit();
            produit.setId(rs.getInt("id"));
            produit.setName(rs.getString("name"));
            produit.setDesciption(rs.getString("desciption"));
            produit.setPrice(rs.getDouble("price"));
            produit.setImagePath(rs.getString("image"));
            produit.setQuantity(rs.getInt("quantity"));
            produit.setCategoryId(rs.getInt("category_id"));
            products.add(produit);
        }

        return products;
    }

    // Get products with 2 or more favorites
    public List<Produit> getProductsWithMultipleFavorites() throws SQLException {
        List<Produit> products = new ArrayList<>();

        // Get products with 2 or more favorites
        String query = "SELECT p.*, COUNT(DISTINCT f.user_id) as favorite_count " +
                "FROM produit p " +
                "JOIN favori f ON p.id = f.produit_id " +
                "GROUP BY p.id " +
                "HAVING favorite_count >= 2";

        Statement stmt = connection.createStatement();
        ResultSet rs = stmt.executeQuery(query);

        while (rs.next()) {
            Produit produit = new Produit();
            produit.setId(rs.getInt("id"));
            produit.setName(rs.getString("name"));
            produit.setDesciption(rs.getString("desciption"));
            produit.setPrice(rs.getDouble("price"));
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

    // Get favorite count for a product
    public int getFavoriteCount(int productId) throws SQLException {
        String query = "SELECT COUNT(DISTINCT user_id) as favorite_count FROM favori WHERE produit_id = ?";
        PreparedStatement ps = connection.prepareStatement(query);
        ps.setInt(1, productId);
        ResultSet rs = ps.executeQuery();

        if (rs.next()) {
            return rs.getInt("favorite_count");
        }
        return 0;
    }

    // Check if there's any product with 2+ favorites in the system
    public boolean existsProductsWithMultipleFavorites() throws SQLException {
        String query = "SELECT COUNT(*) as count FROM " +
                "(SELECT produit_id FROM favori GROUP BY produit_id HAVING COUNT(DISTINCT user_id) >= 2) as popular_products";

        Statement stmt = connection.createStatement();
        ResultSet rs = stmt.executeQuery(query);

        if (rs.next()) {
            return rs.getInt("count") > 0;
        }
        return false;
    }

    /**
     * Check if a product is eligible for the "boost unpopular items" discount.
     * A product is eligible if:
     * 1. There is at least one product with 2 or more favorites AND
     * 2. This product has no favorites at all
     */
    public boolean isProductEligibleForDiscount(int productId) throws SQLException {
        // First check if there are any products with 2+ favorites
        if (existsProductsWithMultipleFavorites()) {
            // There are products with 2+ favorites, so check if this product has NO favorites
            String query = "SELECT COUNT(DISTINCT user_id) as favorite_count FROM favori WHERE produit_id = ?";
            PreparedStatement ps = connection.prepareStatement(query);
            ps.setInt(1, productId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                int favoriteCount = rs.getInt("favorite_count");
                return favoriteCount == 0; // Eligible for discount ONLY if it has NO favorites
            }
            return true; // No favorites for this product, so eligible for discount
        }

        return false; // No products have 2+ favorites, so no discounts apply
    }

    // Get the discounted price for a product (10% discount if eligible)
    public double getDiscountedPrice(int productId) throws SQLException {
        ProduitServices produitServices = new ProduitServices();
        Produit produit = produitServices.getOne(productId);

        if (isProductEligibleForDiscount(productId)) {
            return produit.getPrice() * 0.9; // 10% discount for products with NO favorites
        } else if (isProductInFavorites(0, productId)) {
            // For products in favorites, give a smaller discount (5%)
            return produit.getPrice() * 0.95;
        }

        return produit.getPrice(); // Regular price if not eligible
    }

    /**
     * Get the discount information for a product
     * @param productId The ID of the product
     * @param userId The current user ID
     * @return A map with discount information: { "discountPercent": X, "discountType": "..." }
     */
    public Map<String, Object> getDiscountInfo(int productId, int userId) throws SQLException {
        Map<String, Object> discountInfo = new HashMap<>();

        // Check if product is eligible for no-favorites discount
        boolean isEligibleForDiscount = isProductEligibleForDiscount(productId);

        // Check if product is in user's favorites
        boolean isInFavorites = isProductInFavorites(userId, productId);

        if (isEligibleForDiscount) {
            discountInfo.put("discountPercent", 10);
            discountInfo.put("discountType", "Special promotion for less popular items");
        } else if (isInFavorites) {
            discountInfo.put("discountPercent", 5);
            discountInfo.put("discountType", "Favorite discount");
        } else {
            discountInfo.put("discountPercent", 0);
            discountInfo.put("discountType", "");
        }

        return discountInfo;
    }

    /**
     * Get statistics about favorite counts and discounts
     * @return A map with statistics
     */
    public Map<String, Object> getFavoriteStatistics() throws SQLException {
        Map<String, Object> statistics = new HashMap<>();

        // Get count of products with 2+ favorites
        List<Produit> popularProducts = getProductsWithMultipleFavorites();
        statistics.put("popularProductsCount", popularProducts.size());

        // Get count of products with no favorites
        List<Produit> noFavoriteProducts = getProductsWithNoFavorites();
        statistics.put("noFavoriteProductsCount", noFavoriteProducts.size());

        // Get list of products eligible for discount
        List<Produit> discountedProducts = new ArrayList<>();
        if (existsProductsWithMultipleFavorites()) {
            for (Produit product : noFavoriteProducts) {
                discountedProducts.add(product);
            }
        }
        statistics.put("discountedProductsCount", discountedProducts.size());

        // Calculate total discount value
        double totalDiscountValue = 0.0;
        for (Produit product : discountedProducts) {
            double originalPrice = product.getPrice();
            double discountedPrice = originalPrice * 0.9; // 10% discount
            totalDiscountValue += (originalPrice - discountedPrice);
        }
        statistics.put("totalDiscountValue", totalDiscountValue);

        return statistics;
    }
}