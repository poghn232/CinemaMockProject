package com.example.superapp.dto;

public record UserAdminDto(Long userId, String username, String email, String role, Boolean enabled) {}
