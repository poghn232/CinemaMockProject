package com.example.superapp.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class VideoAssetDto {
    private Long id;
    private String ownerType;
    private Long ownerId;
    private String status;
    private Integer progressPercent;
    private String playbackUrl;
    private String masterPlaylistKey;
    private String errorMessage;
    private Boolean has360p;
    private Boolean has720p;
    private Boolean has1080p;
    private Integer durationSeconds;
    private String originalFileName;


}
