package com.example.superapp.dto;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class LoginResponse {
    private String token;
    private String role;
    private String username;
    private String region;
    private boolean requirePublicIp;

    public boolean isRequirePublicIp() {
        return requirePublicIp;
    }

    public void setRequirePublicIp(boolean requirePublicIp) {
        this.requirePublicIp = requirePublicIp;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public LoginResponse(String token) {
        this.token = token;
    }
    public String getToken() {
        return token;
    }
    public void setToken(String token) {
        this.token = token;
    }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}