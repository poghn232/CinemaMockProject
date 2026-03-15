package com.example.superapp.dto;

// nhận request toggle
public record WishlistToggleRequest(
        Long contentId,
        String contentType
) {}