package com.example.superapp.dto;

public class LoginResponse {
    private String token;
    private String role;
    public LoginResponse(String token) {
        this.token = token;
    }
    public String getToken() {
        return token;
    }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}