package com.event.entities;

import java.sql.Timestamp;

/**
 * Classe représentant l'historique des notifications envoyées aux utilisateurs.
 */
public class NotificationHistory {
    private int id;
    private int userId;
    private int eventId;
    private String notificationType;
    private String details;
    private Timestamp sentDate;
    private boolean isRead;
    private Timestamp readDate;
    
    // Constantes pour les types de notifications
    public static final String TYPE_REMINDER = "REMINDER";
    public static final String TYPE_EVENT_CHANGE = "EVENT_CHANGE";
    public static final String TYPE_REGISTRATION = "REGISTRATION";
    public static final String TYPE_CANCELLATION = "CANCELLATION";
    
    // Constructeurs
    public NotificationHistory() {
    }
    
    public NotificationHistory(int userId, int eventId, String notificationType, String details) {
        this.userId = userId;
        this.eventId = eventId;
        this.notificationType = notificationType;
        this.details = details;
        this.sentDate = new Timestamp(System.currentTimeMillis());
        this.isRead = false;
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
    
    public int getEventId() {
        return eventId;
    }
    
    public void setEventId(int eventId) {
        this.eventId = eventId;
    }
    
    public String getNotificationType() {
        return notificationType;
    }
    
    public void setNotificationType(String notificationType) {
        this.notificationType = notificationType;
    }
    
    public String getDetails() {
        return details;
    }
    
    public void setDetails(String details) {
        this.details = details;
    }
    
    public Timestamp getSentDate() {
        return sentDate;
    }
    
    public void setSentDate(Timestamp sentDate) {
        this.sentDate = sentDate;
    }
    
    public boolean isRead() {
        return isRead;
    }
    
    public void setRead(boolean read) {
        isRead = read;
        if (read) {
            this.readDate = new Timestamp(System.currentTimeMillis());
        }
    }
    
    public Timestamp getReadDate() {
        return readDate;
    }
    
    public void setReadDate(Timestamp readDate) {
        this.readDate = readDate;
    }
    
    @Override
    public String toString() {
        return "NotificationHistory{" +
                "id=" + id +
                ", userId=" + userId +
                ", eventId=" + eventId +
                ", notificationType='" + notificationType + '\'' +
                ", sentDate=" + sentDate +
                ", isRead=" + isRead +
                '}';
    }
} 