package com.services;

import com.models.Commande;
import com.models.CommandeLigne;
import com.models.Produit;
import com.utils.DataSource;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class CommandeService {
    private Connection connection;
    private ProduitServices produitServices;

    public CommandeService() {
        connection = DataSource.getInstance().getConnection();
        produitServices = new ProduitServices();
    }

    /**
     * Create a new order or get an existing cart for a user
     * @param userId The ID of the user
     * @return The Commande object representing the cart
     */
    public Commande getOrCreateCart(int userId) throws SQLException {
        // Check if user has an existing cart (commande with status='cart')
        String query = "SELECT * FROM commande WHERE user_id = ? AND statut = 'cart'";

        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, userId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    // Existing cart found
                    Commande cart = new Commande();
                    cart.setId(rs.getInt("id"));
                    cart.setUserId(rs.getInt("user_id"));
                    cart.setDateCommande(rs.getObject("date_commande", LocalDate.class));
                    cart.setStatut(rs.getString("statut"));
                    cart.setTotal(rs.getDouble("total"));
                    return cart;
                } else {
                    // No cart exists, create a new one
                    return createNewCart(userId);
                }
            }
        }
    }

    /**
     * Create a new cart for a user
     * @param userId The ID of the user
     * @return The newly created Commande object
     */
    private Commande createNewCart(int userId) throws SQLException {
        String insertQuery = "INSERT INTO commande (user_id, date_commande, statut, total) VALUES (?, ?, 'cart', 0)";

        try (PreparedStatement ps = connection.prepareStatement(insertQuery, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, userId);
            ps.setDate(2, Date.valueOf(LocalDate.now()));
            ps.executeUpdate();

            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    Commande newCart = new Commande();
                    newCart.setId(generatedKeys.getInt(1));
                    newCart.setUserId(userId);
                    newCart.setDateCommande(LocalDate.now());
                    newCart.setStatut("cart");
                    newCart.setTotal(0);
                    return newCart;
                } else {
                    throw new SQLException("Failed to create new cart, no ID obtained.");
                }
            }
        }
    }
    public boolean addToCart(int userId, int productId, int quantity) throws SQLException {
        return addToCart(userId, productId, quantity, false, 0);
    }
    /**
     * Add a product to the user's cart
     * @param userId The ID of the user
     * @param productId The ID of the product to add
     * @param quantity The quantity to add
     * @return True if successfully added
     */
    public boolean addToCart(int userId, int productId, int quantity, boolean discounted, double discountedPrice) throws SQLException {
        try {
            // First, ensure the user has a cart
            Commande cart = getOrCreateCart(userId);

            // Check if the product is already in the cart
            String checkQuery = "SELECT * FROM commande_ligne WHERE commande_id = ? AND produit_id = ?";
            PreparedStatement checkStatement = connection.prepareStatement(checkQuery);
            checkStatement.setInt(1, cart.getId());
            checkStatement.setInt(2, productId);
            ResultSet checkResult = checkStatement.executeQuery();

            // Get the price to use (original or discounted)
            double priceToUse = discounted ? discountedPrice : getProduitPrice(productId);

            if (checkResult.next()) {
                // Product already in cart, update quantity
                int currentQuantity = checkResult.getInt("quantity");
                int newQuantity = currentQuantity + quantity;

                String updateQuery = "UPDATE commande_ligne SET quantity = ? WHERE commande_id = ? AND produit_id = ?";
                PreparedStatement updateStatement = connection.prepareStatement(updateQuery);
                updateStatement.setInt(1, newQuantity);
                updateStatement.setInt(2, cart.getId());
                updateStatement.setInt(3, productId);
                updateStatement.executeUpdate();
            } else {
                // Add new product to cart
                String insertQuery = "INSERT INTO commande_ligne (commande_id, produit_id, quantity, price_at_purchase) VALUES (?, ?, ?, ?)";
                PreparedStatement insertStatement = connection.prepareStatement(insertQuery);
                insertStatement.setInt(1, cart.getId());
                insertStatement.setInt(2, productId);
                insertStatement.setInt(3, quantity);
                insertStatement.setDouble(4, priceToUse);  // Store the discounted price if applicable
                insertStatement.executeUpdate();
            }

            // Update the cart total
            updateCartTotal(cart.getId());
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }


    /**
     * Update the quantity of an item in the cart
     * @param lineId The ID of the cart line
     * @param quantity The new quantity
     * @return True if successfully updated
     */
    public boolean updateCartItemQuantity(int lineId, int quantity) throws SQLException {
        String query = "UPDATE commande_ligne SET quantity = ? WHERE id = ?";

        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, quantity);
            ps.setInt(2, lineId);

            int rowsAffected = ps.executeUpdate();

            if (rowsAffected > 0) {
                // Update the total in the commande
                updateCartTotal(getCommandeIdByLineId(lineId));
                return true;
            }
            return false;
        }
    }

    /**
     * Add a new item to the cart
     * @param commandeId The ID of the order/cart
     * @param productId The ID of the product
     * @param quantity The quantity
     * @return True if successfully added
     */
    private boolean addNewCartItem(int commandeId, int productId, int quantity) throws SQLException {
        String query = "INSERT INTO commande_ligne (commande_id, produit_id, quantity) VALUES (?, ?, ?)";

        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, commandeId);
            ps.setInt(2, productId);
            ps.setInt(3, quantity);

            int rowsAffected = ps.executeUpdate();

            if (rowsAffected > 0) {
                // Update the total in the commande
                updateCartTotal(commandeId);
                return true;
            }
            return false;
        }
    }

    /**
     * Get the commande ID for a given commande_ligne
     * @param lineId The ID of the commande_ligne
     * @return The associated commande ID
     */
    private int getCommandeIdByLineId(int lineId) throws SQLException {
        String query = "SELECT commande_id FROM commande_ligne WHERE id = ?";

        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, lineId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("commande_id");
                }
                throw new SQLException("Cart item not found.");
            }
        }
    }

    /**
     * Update the total value of the cart
     * @param commandeId The ID of the order/cart
     */
    public void updateCartTotal(int commandeId) throws SQLException {
        // Modified to explicitly use price_at_purchase from commande_ligne
        String totalQuery = "SELECT SUM(cl.price_at_purchase * cl.quantity) AS cart_total " +
                "FROM commande_ligne cl " +
                "WHERE cl.commande_id = ?";

        try (PreparedStatement ps = connection.prepareStatement(totalQuery)) {
            ps.setInt(1, commandeId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    double total = rs.getDouble("cart_total");

                    // Update the total in the commande
                    String updateQuery = "UPDATE commande SET total = ? WHERE id = ?";
                    try (PreparedStatement updatePs = connection.prepareStatement(updateQuery)) {
                        updatePs.setDouble(1, total);
                        updatePs.setInt(2, commandeId);
                        updatePs.executeUpdate();
                    }
                }
            }
        }
    }
    /**
     * Remove an item from the cart
     * @param userId The ID of the user
     * @param productId The ID of the product to remove
     * @return True if successfully removed
     */
    public boolean removeFromCart(int userId, int productId) throws SQLException {
        // Get the user's cart
        Commande cart = getOrCreateCart(userId);

        String query = "DELETE FROM commande_ligne WHERE commande_id = ? AND produit_id = ?";

        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, cart.getId());
            ps.setInt(2, productId);

            int rowsAffected = ps.executeUpdate();

            if (rowsAffected > 0) {
                // Update the total in the commande
                updateCartTotal(cart.getId());
                return true;
            }
            return false;
        }
    }

    /**
     * Get all cart items for a user
     * @param userId The ID of the user
     * @return List of products in the user's cart with quantities
     */
    public List<CartItem> getCartItems(int userId) throws SQLException {
        List<CartItem> cartItems = new ArrayList<>();

        // Get the user's cart
        Commande cart = getOrCreateCart(userId);

        String query = "SELECT cl.id, cl.produit_id, cl.quantity, cl.price_at_purchase, p.name, p.price, p.image, p.desciption " +
                "FROM commande_ligne cl " +
                "JOIN produit p ON cl.produit_id = p.id " +
                "WHERE cl.commande_id = ?";

        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, cart.getId());

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Produit produit = new Produit();
                    produit.setId(rs.getInt("produit_id"));
                    produit.setName(rs.getString("name"));
                    produit.setPrice(rs.getDouble("price"));
                    produit.setImagePath(rs.getString("image"));
                    produit.setDesciption(rs.getString("desciption"));

                    CartItem item = new CartItem();
                    item.setId(rs.getInt("id"));
                    item.setProduit(produit);
                    item.setQuantity(rs.getInt("quantity"));
                    item.setPrice_at_purchase(rs.getDouble("price_at_purchase"));

                    cartItems.add(item);
                }
            }
        }

        return cartItems;
    }
    private double getProduitPrice(int productId) throws SQLException {
        String query = "SELECT price FROM produit WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("price");
                }
                throw new SQLException("Product not found");
            }
        }
    }
    /**
     * Clear all items from a user's cart
     * @param userId The ID of the user
     * @return True if successfully cleared
     */
    public boolean clearCart(int userId) throws SQLException {
        Commande cart = getOrCreateCart(userId);

        String query = "DELETE FROM commande_ligne WHERE commande_id = ?";

        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, cart.getId());

            ps.executeUpdate();

            // Reset the total
            String updateQuery = "UPDATE commande SET total = 0 WHERE id = ?";
            try (PreparedStatement updatePs = connection.prepareStatement(updateQuery)) {
                updatePs.setInt(1, cart.getId());
                updatePs.executeUpdate();
            }

            return true;
        }
    }
    public boolean addToCartWithPrice(int userId, int productId, int quantity, double price) throws SQLException {
        // First, check if there's an existing "cart" commande (order in "panier" status)
        String checkCommandeQuery = "SELECT id FROM commande WHERE user_id = ? AND statut = 'panier'";
        int commandeId = -1;

        try (PreparedStatement ps = connection.prepareStatement(checkCommandeQuery)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                // There is an existing cart
                commandeId = rs.getInt("id");
            } else {
                // Create a new commande as cart
                String createCommandeQuery = "INSERT INTO commande (user_id, date_commande, statut, total) VALUES (?, ?, 'panier', 0)";
                try (PreparedStatement createPs = connection.prepareStatement(createCommandeQuery, PreparedStatement.RETURN_GENERATED_KEYS)) {
                    createPs.setInt(1, userId);
                    createPs.setDate(2, java.sql.Date.valueOf(java.time.LocalDate.now()));
                    createPs.executeUpdate();

                    ResultSet generatedKeys = createPs.getGeneratedKeys();
                    if (generatedKeys.next()) {
                        commandeId = generatedKeys.getInt(1);
                    } else {
                        throw new SQLException("Creating cart failed, no ID obtained.");
                    }
                }
            }

            // Now check if this product already exists in commande_ligne for this commande
            String checkLineQuery = "SELECT id, quantity FROM commande_ligne WHERE commande_id = ? AND produit_id = ?";
            try (PreparedStatement linePs = connection.prepareStatement(checkLineQuery)) {
                linePs.setInt(1, commandeId);
                linePs.setInt(2, productId);
                ResultSet lineRs = linePs.executeQuery();

                if (lineRs.next()) {
                    // Update existing line
                    int lineId = lineRs.getInt("id");
                    int currentQuantity = lineRs.getInt("quantity");
                    String updateLineQuery = "UPDATE commande_ligne SET quantity = ? WHERE id = ?";
                    try (PreparedStatement updatePs = connection.prepareStatement(updateLineQuery)) {
                        updatePs.setInt(1, currentQuantity + quantity);
                        updatePs.setInt(2, lineId);
                        updatePs.executeUpdate();
                    }
                } else {
                    // Insert new line without price column
                    String insertLineQuery = "INSERT INTO commande_ligne (commande_id, produit_id, quantity) VALUES (?, ?, ?)";
                    try (PreparedStatement insertPs = connection.prepareStatement(insertLineQuery)) {
                        insertPs.setInt(1, commandeId);
                        insertPs.setInt(2, productId);
                        insertPs.setInt(3, quantity);
                        insertPs.executeUpdate();
                    }
                }

                // Update the total in the commande - we'll use the provided price for the calculation
                updateCommandeTotalWithDiscount(commandeId, productId, price);

                return true;
            }
        }
    }

    private void updateCommandeTotalWithDiscount(int commandeId, int productId, double price) {
    }

    private void updateCommandeTotal(int commandeId) throws SQLException {
        // Calculate the total from all line items
        String totalQuery = "SELECT SUM(cl.quantity * cl.price) as total " +
                "FROM commande_ligne cl " +
                "WHERE cl.commande_id = ?";

        try (PreparedStatement ps = connection.prepareStatement(totalQuery)) {
            ps.setInt(1, commandeId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                double total = rs.getDouble("total");

                // Update the commande total
                String updateQuery = "UPDATE commande SET total = ? WHERE id = ?";
                try (PreparedStatement updatePs = connection.prepareStatement(updateQuery)) {
                    updatePs.setDouble(1, total);
                    updatePs.setInt(2, commandeId);
                    updatePs.executeUpdate();
                }
            }
        }
    }
    /**
     * A class to represent a cart item with product and quantity
     */
    public static class CartItem {
        private int id;
        private Produit produit;
        private int quantity;
        private double price_at_purchase; // Add this field

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public Produit getProduit() {
            return produit;
        }

        public void setProduit(Produit produit) {
            this.produit = produit;
        }

        public int getQuantity() {
            return quantity;
        }

        public void setQuantity(int quantity) {
            this.quantity = quantity;
        }

        public double getPrice_at_purchase() {
            return price_at_purchase;
        }

        public void setPrice_at_purchase(double price_at_purchase) {
            this.price_at_purchase = price_at_purchase;
        }

        public double getSubtotal() {
            // Use price_at_purchase if it's set, otherwise use product price
            double priceToUse = price_at_purchase > 0 ? price_at_purchase : produit.getPrice();
            return priceToUse * quantity;
        }
    }
    private int getActiveCartId(int userId) throws SQLException {
        String sql = "SELECT id FROM commande WHERE user_id = ? AND status = 'cart'";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
        }
        return -1;
    }
}