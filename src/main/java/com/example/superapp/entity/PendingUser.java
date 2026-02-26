package com.example.superapp.entity;

public class PendingUser {

    private String email;
    private String encodedPassword;
    private long expireTime;

    public PendingUser(String email, String encodedPassword, long expireTime) {
        this.email = email;
        this.encodedPassword = encodedPassword;
        this.expireTime = expireTime;
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