package com.example.superapp.dto;

// nhận request toggle
public record WishlistToggleRequest(
        long profileId,
        Long contentId,
        String contentType
) {}