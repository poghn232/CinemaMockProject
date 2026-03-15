package com.example.superapp.dto;

import java.time.LocalDateTime;

// trả về khi get danh sách
public record WishlistItemDto(
        Long id,
        Long contentId,
        String contentType,
        LocalDateTime addedAt,
        String title,
        String posterUrl,
        Double rating,
        Integer year
) {}