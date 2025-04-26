package com.services;


import com.utils.DataSource;
import com.models.*;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProduitServices {
    private Connection connection;

    public ProduitServices() {
        connection = DataSource.getInstance().getConnection();
    }

    /**
     * Check if a product with the given name already exists
     * @param name The product name to check
     * @return true if the product name already exists, false otherwise
     * @throws SQLException if a database error occurs
     */
    public boolean productNameExists(String name) throws SQLException {
        String query = "SELECT COUNT(*) FROM produit WHERE name = ?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    /**
     * Check if a product with the given name already exists, excluding the current product
     * @param name The product name to check
     * @param productId The ID of the current product to exclude from the check
     * @return true if another product with the same name exists, false otherwise
     * @throws SQLException if a database error occurs
     */
    public boolean productNameExistsExcludingCurrent(String name, int productId) throws SQLException {
        String query = "SELECT COUNT(*) FROM produit WHERE name = ? AND id != ?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, name);
            ps.setInt(2, productId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    public int insert(Produit produit) throws SQLException {
        // Check if product with the same name already exists
        if (productNameExists(produit.getName())) {
            throw new SQLException("A product with this name already exists");
        }

        String query = "INSERT INTO produit (category_id, name, desciption, price, image, quantity) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, produit.getCategoryId());
            ps.setString(2, produit.getName());
            ps.setString(3, produit.getDescription());
            ps.setDouble(4, produit.getPrice());
            ps.setString(5, produit.getImagePath());
            ps.setInt(6, produit.getQuantity());

            ps.executeUpdate();

            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                } else {
                    throw new SQLException("Failed to get inserted ID");
                }
            }
        }
    }

    public void update(Produit produit) throws SQLException {
        // Check if another product with the same name already exists
        if (productNameExistsExcludingCurrent(produit.getName(), produit.getId())) {
            throw new SQLException("Another product with this name already exists");
        }

        String query = "UPDATE produit SET category_id = ?, name = ?, desciption = ?, price = ?, image = ?, quantity = ? WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, produit.getCategoryId());
            ps.setString(2, produit.getName());
            ps.setString(3, produit.getDescription());
            ps.setDouble(4, produit.getPrice());
            ps.setString(5, produit.getImagePath());
            ps.setInt(6, produit.getQuantity());
            ps.setInt(7, produit.getId());

            ps.executeUpdate();
        }
    }

    // Rest of your original methods remain unchanged
    public void delete(Produit produit) throws SQLException {
        String query = "DELETE FROM produit WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, produit.getId());
            ps.executeUpdate();
        }
    }

    public List<Produit> showAll() throws SQLException {
        List<Produit> produits = new ArrayList<>();
        String query = "SELECT * FROM produit";
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(query)) {

            while (resultSet.next()) {
                Produit produit = new Produit();
                produit.setId(resultSet.getInt("id"));
                produit.setCategoryId(resultSet.getInt("category_id"));
                produit.setName(resultSet.getString("name"));
                produit.setDescription(resultSet.getString("desciption"));
                produit.setPrice(resultSet.getDouble("price"));
                produit.setImagePath(resultSet.getString("image"));
                produit.setQuantity(resultSet.getInt("quantity"));

                produits.add(produit);
            }
        }
        return produits;
    }

    public Produit getOne(int id) throws SQLException {
        String query = "SELECT * FROM produit WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, id);
            try (ResultSet resultSet = ps.executeQuery()) {
                if (resultSet.next()) {
                    Produit produit = new Produit();
                    produit.setId(resultSet.getInt("id"));
                    produit.setCategoryId(resultSet.getInt("category_id"));
                    produit.setName(resultSet.getString("name"));
                    produit.setDescription(resultSet.getString("desciption"));
                    produit.setPrice(resultSet.getDouble("price"));
                    produit.setImagePath(resultSet.getString("image"));
                    produit.setQuantity(resultSet.getInt("quantity"));
                    return produit;
                }
            }
        }
        return null;
    }

    public List<Produit> getByCategory(int categoryId) throws SQLException {
        List<Produit> produits = new ArrayList<>();
        String query = "SELECT * FROM produit WHERE category_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, categoryId);
            try (ResultSet resultSet = ps.executeQuery()) {
                while (resultSet.next()) {
                    Produit produit = new Produit();
                    produit.setId(resultSet.getInt("id"));
                    produit.setCategoryId(resultSet.getInt("category_id"));
                    produit.setName(resultSet.getString("name"));
                    produit.setDescription(resultSet.getString("desciption"));
                    produit.setPrice(resultSet.getDouble("price"));
                    produit.setImagePath(resultSet.getString("image"));
                    produit.setQuantity(resultSet.getInt("quantity"));

                    produits.add(produit);
                }
            }
        }
        return produits;
    }
}