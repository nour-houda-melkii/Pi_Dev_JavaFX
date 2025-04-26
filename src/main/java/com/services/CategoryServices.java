package com.services;


import com.utils.DataSource;
import com.models.*;
import com.utils.*;


import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CategoryServices {
    private Connection connection;

    public CategoryServices() {
        connection = DataSource.getInstance().getConnection();
    }

    /**
     * Check if a category with the given name already exists
     * @param name The category name to check
     * @return true if the category name already exists, false otherwise
     * @throws SQLException if a database error occurs
     */
    public boolean categoryNameExists(String name) throws SQLException {
        String query = "SELECT COUNT(*) FROM category WHERE name = ?";
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
     * Check if a category with the given name already exists, excluding the current category
     * @param name The category name to check
     * @param categoryId The ID of the current category to exclude from the check
     * @return true if another category with the same name exists, false otherwise
     * @throws SQLException if a database error occurs
     */
    public boolean categoryNameExistsExcludingCurrent(String name, int categoryId) throws SQLException {
        String query = "SELECT COUNT(*) FROM category WHERE name = ? AND id != ?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, name);
            ps.setInt(2, categoryId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    public int insert(Category category) throws SQLException {
        // Check if category with the same name already exists
        if (categoryNameExists(category.getName())) {
            throw new SQLException("A category with this name already exists");
        }

        String query = "INSERT INTO category (name, description) VALUES (?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, category.getName());
            ps.setString(2, category.getDescription());

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

    public void update(Category category) throws SQLException {
        // Check if another category with the same name already exists
        if (categoryNameExistsExcludingCurrent(category.getName(), category.getId())) {
            throw new SQLException("Another category with this name already exists");
        }

        String query = "UPDATE category SET name = ?, description = ? WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, category.getName());
            ps.setString(2, category.getDescription());
            ps.setInt(3, category.getId());

            ps.executeUpdate();
        }
    }

    // Rest of your original methods remain unchanged
    public void delete(Category category) throws SQLException {
        String query = "DELETE FROM category WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, category.getId());
            ps.executeUpdate();
        }
    }

    public List<Category> showAll() throws SQLException {
        List<Category> categories = new ArrayList<>();
        String query = "SELECT * FROM category";
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(query)) {

            while (resultSet.next()) {
                Category category = new Category();
                category.setId(resultSet.getInt("id"));
                category.setName(resultSet.getString("name"));
                category.setDescription(resultSet.getString("description"));

                categories.add(category);
            }
        }
        return categories;
    }

    public Category getOne(int id) throws SQLException {
        String query = "SELECT * FROM category WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, id);
            try (ResultSet resultSet = ps.executeQuery()) {
                if (resultSet.next()) {
                    Category category = new Category();
                    category.setId(resultSet.getInt("id"));
                    category.setName(resultSet.getString("name"));
                    category.setDescription(resultSet.getString("description"));
                    return category;
                }
            }
        }
        return null;
    }
}