package com.models;

public class Message {
    public String messageId;
    public int senderId;
    public String content;
    public long timestamp;

    public Message() {}

    public Message(String messageId, int senderId, String content, long timestamp) {
        this.messageId = messageId;
        this.senderId = senderId;
        this.content = content;
        this.timestamp = timestamp;
    }
}

