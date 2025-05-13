package com.models;

import java.time.LocalDateTime;

public class LoginHistory {
    private int id;
    private int userId;
    private LocalDateTime loginTime;

    // Constructeurs
    public LoginHistory() {
    }

    public LoginHistory(int userId, LocalDateTime loginTime) {
        this.userId = userId;
        this.loginTime = loginTime;
    }

    public LoginHistory(int id, int userId, LocalDateTime loginTime) {
        this.id = id;
        this.userId = userId;
        this.loginTime = loginTime;
    }

    // Getters et Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public LocalDateTime getLoginTime() {
        return loginTime;
    }

    public void setLoginTime(LocalDateTime loginTime) {
        this.loginTime = loginTime;
    }

    // Méthode toString()
    @Override
    public String toString() {
        return "LoginHistory{" +
                "id=" + id +
                ", userId=" + userId +
                ", loginTime=" + loginTime +
                '}';
    }
}