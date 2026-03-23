package com.example.superapp.entity;

import com.example.superapp.dto.VideoAssetDto;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "video_assets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VideoAsset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // movie | tv_episode
    @Column(nullable = false, length = 30)
    private String ownerType;

    // movieId hoặc episodeId
    @Column(nullable = false)
    private Long ownerId;

    @Column(nullable = false, length = 255)
    private String originalFileName;

    @Column(nullable = false, length = 500)
    private String localSourcePath;

    @Column(length = 500)
    private String sourceObjectKey;

    @Column(length = 500)
    private String masterPlaylistKey;

    @Column(length = 1000)
    private String playbackUrl;

    @Column(length = 30)
    private String status; // UPLOADED, PROCESSING, READY, FAILED

    @Column(length = 2000)
    private String errorMessage;

    private Long fileSizeBytes;
    private Integer durationSeconds;
    private Integer sourceWidth;
    private Integer sourceHeight;

    private Boolean has360p;
    private Boolean has720p;
    private Boolean has1080p;

    private Integer progressPercent;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) status = "UPLOADED";
        if (progressPercent == null) progressPercent = 0;
        if (has360p == null) has360p = false;
        if (has720p == null) has720p = false;
        if (has1080p == null) has1080p = false;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    private VideoAssetDto toDto(VideoAsset asset) {
        return VideoAssetDto.builder()
                .id(asset.getId())
                .ownerType(asset.getOwnerType())
                .ownerId(asset.getOwnerId())
                .status(asset.getStatus())
                .progressPercent(asset.getProgressPercent())
                .playbackUrl(asset.getPlaybackUrl())
                .masterPlaylistKey(asset.getMasterPlaylistKey())
                .errorMessage(asset.getErrorMessage())
                .has360p(asset.getHas360p())
                .has720p(asset.getHas720p())
                .has1080p(asset.getHas1080p())
                .durationSeconds(asset.getDurationSeconds())
                .originalFileName(asset.getOriginalFileName())
                .build();
    }
}