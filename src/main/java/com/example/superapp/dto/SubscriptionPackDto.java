package com.example.superapp.dto;

public record SubscriptionPackDto(Long packId, String packName, String packPrice, Integer durationDays, Integer maxProfiles) {}
