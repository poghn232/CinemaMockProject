package com.example.superapp.dto;

// Added `premium` so the admin UI can show user-level premium status (true if user has an active subscription)
public record UserAdminDto(Long userId, String username, String email, String role, Boolean premium, Boolean commentDisabled) {}
