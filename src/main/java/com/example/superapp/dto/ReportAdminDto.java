package com.example.superapp.dto;

import java.time.LocalDateTime;

public class ReportAdminDto {
    public Long id;
    public Long reviewId;
    public String reportedUsername;
    public String reporterUsername;
    public String reason;
    public LocalDateTime createdAt;
    public String status;
    public String adminReason;
    public LocalDateTime adminActionAt;
    public String adminUsername;
    public String comment; // the reported review/comment text

    public ReportAdminDto() {}
}
