package com.example.superapp.dto;

import java.time.LocalDateTime;

public class ReportAdminDto {
    public Long id;
    public Long reviewId;
    public String reportedProfile;
    public String reporterProfile;
    public String reason;
    public LocalDateTime createdAt;
    public String status;
    public String adminReason;
    public LocalDateTime adminActionAt;

    public ReportAdminDto() {}
}
