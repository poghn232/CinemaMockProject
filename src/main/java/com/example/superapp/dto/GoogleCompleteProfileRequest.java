package com.example.superapp.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GoogleCompleteProfileRequest {
    private String email;
    private String username;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
