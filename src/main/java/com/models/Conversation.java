package com.models;

public class Conversation {

    public String conversationId;
    public int postId;
    public int postOwnerId;
    public int interestedUserId;
    public String status; // "open", "validated", "cancelled"

    public Conversation() {}

    public Conversation(String conversationId, int postId, int postOwnerId, int interestedUserId, String status) {
        this.conversationId = conversationId;
        this.postId = postId;
        this.postOwnerId = postOwnerId;
        this.interestedUserId = interestedUserId;
        this.status = status;
    }
    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public int getPostId() {
        return postId;
    }

    public void setPostId(int postId) {
        this.postId = postId;
    }

    public int getPostOwnerId() {
        return postOwnerId;
    }

    public void setPostOwnerId(int postOwnerId) {
        this.postOwnerId = postOwnerId;
    }

    public int getInterestedUserId() {
        return interestedUserId;
    }

    public void setInterestedUserId(int interestedUserId) {
        this.interestedUserId = interestedUserId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
