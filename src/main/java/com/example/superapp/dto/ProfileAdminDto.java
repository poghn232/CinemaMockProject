package com.example.superapp.dto;

public record ProfileAdminDto(Long profileId, String profileName, String email, String role, Boolean commentDisabled) {}
