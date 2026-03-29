package com.example.superapp.dto;

import lombok.Data;

@Data
public class CreateFromR2Request {
    private String title;
    private String adType;
    private String status;
    private Boolean skippable;
    private Integer skipAfterSeconds;
    private Long sourceAdId;
}