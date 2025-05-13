package com.event.models;

import java.time.LocalDateTime;

public class Inscription {
    private int id;
    private int userId;
    private int eventId;
    private LocalDateTime dateInscription;
    private boolean hasUnsubscribed;
    
    // Références aux objets liés (optionnel)
    private User user;
    private Event event;
    
    // Constructeur vide
    public Inscription() {
        this.dateInscription = LocalDateTime.now();
        this.hasUnsubscribed = false;
    }
    
    // Constructeur avec champs essentiels
    public Inscription(int userId, int eventId) {
        this();
        this.userId = userId;
        this.eventId = eventId;
    }
    
    // Constructeur complet
    public Inscription(int id, int userId, int eventId, LocalDateTime dateInscription, boolean hasUnsubscribed) {
        this.id = id;
        this.userId = userId;
        this.eventId = eventId;
        this.dateInscription = dateInscription;
        this.hasUnsubscribed = hasUnsubscribed;
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
    
    public LocalDateTime getDateInscription() {
        return dateInscription;
    }
    
    public void setDateInscription(LocalDateTime dateInscription) {
        this.dateInscription = dateInscription;
    }
    
    public boolean isHasUnsubscribed() {
        return hasUnsubscribed;
    }
    
    public void setHasUnsubscribed(boolean hasUnsubscribed) {
        this.hasUnsubscribed = hasUnsubscribed;
    }
    
    public User getUser() {
        return user;
    }
    
    public void setUser(User user) {
        this.user = user;
        if (user != null) {
            this.userId = user.getId();
        }
    }
    
    public Event getEvent() {
        return event;
    }
    
    public void setEvent(Event event) {
        this.event = event;
        if (event != null) {
            this.eventId = event.getId();
        }
    }
    
    // Méthode pour désinscrire un utilisateur
    public void unsubscribe() {
        this.hasUnsubscribed = true;
    }
    
    @Override
    public String toString() {
        return "Inscription{" +
                "id=" + id +
                ", userId=" + userId +
                ", eventId=" + eventId +
                ", dateInscription=" + dateInscription +
                ", hasUnsubscribed=" + hasUnsubscribed +
                '}';
    }
} 