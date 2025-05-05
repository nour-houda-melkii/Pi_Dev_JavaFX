package com.services;

import com.models.Commentaire;
import com.utils.DataSource;
import com.utils.DataSource;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CommentaireService {
    private Connection connection;

    public CommentaireService() {
        connection = DataSource.getInstance().getConnection();    }

    // Add a new comment
    public void addComment(Commentaire comment) throws SQLException {
        String query = "INSERT INTO commentaire (produit_id, user_id, content, created_at) VALUES (?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, comment.getProduitId());
            statement.setInt(2, comment.getUserId());
            statement.setString(3, comment.getContent());
            statement.setTimestamp(4, new Timestamp(System.currentTimeMillis()));

            statement.executeUpdate();

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    comment.setId(generatedKeys.getInt(1));
                }
            }
        }
    }

    // Update a comment
    public void updateComment(Commentaire comment) throws SQLException {
        String query = "UPDATE commentaire SET content = ? WHERE id = ? AND user_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, comment.getContent());
            statement.setInt(2, comment.getId());
            statement.setInt(3, comment.getUserId());
            statement.executeUpdate();
        }
    }

    // Delete a comment
    public void deleteComment(int commentId, int userId) throws SQLException {
        String query = "DELETE FROM commentaire WHERE id = ? AND user_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, commentId);
            statement.setInt(2, userId);
            statement.executeUpdate();
        }
    }

    // Get all comments for a product
    public List<Commentaire> getCommentsForProduct(int produitId) throws SQLException {
        List<Commentaire> comments = new ArrayList<>();
        String query = "SELECT c.* FROM commentaire c " +
                "WHERE c.produit_id = ? ORDER BY c.created_at DESC";

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, produitId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    Commentaire comment = new Commentaire();
                    comment.setId(resultSet.getInt("id"));
                    comment.setProduitId(resultSet.getInt("produit_id"));
                    comment.setUserId(resultSet.getInt("user_id"));
                    comment.setContent(resultSet.getString("content"));
                    comment.setCreatedAt(resultSet.getTimestamp("created_at"));
                    comments.add(comment);
                }
            }
        }
        return comments;
    }

    // Get comment count for each product
    public List<ProductCommentCount> getProductCommentCounts() throws SQLException {
        List<ProductCommentCount> counts = new ArrayList<>();
        String query = "SELECT produit_id, COUNT(*) as comment_count FROM commentaire GROUP BY produit_id ORDER BY comment_count DESC";

        try (PreparedStatement statement = connection.prepareStatement(query);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                counts.add(new ProductCommentCount(
                        resultSet.getInt("produit_id"),
                        resultSet.getInt("comment_count")
                ));
            }
        }
        return counts;
    }

    // Helper class for product comment counts
    public static class ProductCommentCount {
        private int produitId;
        private int commentCount;

        public ProductCommentCount(int produitId, int commentCount) {
            this.produitId = produitId;
            this.commentCount = commentCount;
        }

        public int getProduitId() {
            return produitId;
        }

        public int getCommentCount() {
            return commentCount;
        }
    }
}