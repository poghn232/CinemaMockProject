package com.example.superapp.dto;

import java.time.LocalDateTime;

public record NotificationDto(
        Long id,
        String messageKey,
        String messageParams,
        LocalDateTime createdAt,
        boolean isRead,
        Long contentId,
        String contentType,
        String contentTitle,
        String posterUrl,
        String eventType,
        Long episodeId,
        String iconUrl
) {}