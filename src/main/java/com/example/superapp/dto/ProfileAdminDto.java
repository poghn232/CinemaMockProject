package com.example.superapp.dto;

// Profile-level `premium` removed: premium is shown at user-level only now.
public record ProfileAdminDto(Long profileId, String profileName, String email, String role, Boolean commentDisabled) {}
