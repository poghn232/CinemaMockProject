package com.example.superapp.entity;

public class PendingUser {

    private String email;
    private String encodedPassword;
    private long expireTime;
    private String username;

    public PendingUser(String email, String encodedPassword, long expireTime,String username) {
        this.email = email;
        this.encodedPassword = encodedPassword;
        this.expireTime = expireTime;
        this.username = username;
    }

    public String getUsername() {
        return username;
    }
    public void setUsername(String username) {
        this.username = username;
    }
    public String getEmail() {
        return email;
    }

    public String getEncodedPassword() {
        return encodedPassword;
    }

    public long getExpireTime() {
        return expireTime;
    }
}