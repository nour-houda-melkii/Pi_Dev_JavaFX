package Utils;

import entities.user;

public class UserSession {
    private static UserSession instance;
    private user currentUser;
    private int currentOrderId = -1; // To track current cart/order ID

    private UserSession() {
        // Private constructor for singleton
    }

    public static synchronized UserSession getInstance() {
        if (instance == null) {
            instance = new UserSession();
        }
        return instance;
    }

    public user getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(user user) {
        this.currentUser = user;
    }

    public int getCurrentOrderId() {
        return currentOrderId;
    }

    public void setCurrentOrderId(int orderId) {
        this.currentOrderId = orderId;
    }

    public boolean isLoggedIn() {
        return currentUser != null;
    }

    public void logout() {
        currentUser = null;
        currentOrderId = -1;
    }
}