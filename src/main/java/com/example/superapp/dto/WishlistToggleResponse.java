package com.example.superapp.dto;

// trả về sau toggle/check
public record WishlistToggleResponse(
        boolean wishlisted,
        String message
) {}