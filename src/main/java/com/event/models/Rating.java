package com.event.models;

public class Rating {
    private int id;
    private int userId;
    private int eventId;
    private double rating;
    
    // Références aux objets liés (optionnel)
    private User user;
    private Event event;
    
    // Constructeur vide
    public Rating() {
    }
    
    // Constructeur avec champs essentiels
    public Rating(int userId, int eventId, double rating) {
        this.userId = userId;
        this.eventId = eventId;
        this.rating = rating;
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
    
    public double getRating() {
        return rating;
    }
    
    public void setRating(double rating) {
        this.rating = rating;
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
    
    @Override
    public String toString() {
        return "Rating{" +
                "id=" + id +
                ", userId=" + userId +
                ", eventId=" + eventId +
                ", rating=" + rating +
                '}';
    }
} 