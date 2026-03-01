package com.example.superapp.dto;

import java.math.BigDecimal;

public record SubscriptionPackDto(Long packId, String packName, BigDecimal packPrice, Integer durationDays) {}
