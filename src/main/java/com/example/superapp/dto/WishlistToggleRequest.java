package com.example.superapp.dto;

// nhận request toggle
public record WishlistToggleRequest(
        Long profileId,
        Long contentId,
        String contentType
) {}