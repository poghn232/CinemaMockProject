package com.example.superapp.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreatePaymentRequest {
    private Long userId;
    private Long packId;
}