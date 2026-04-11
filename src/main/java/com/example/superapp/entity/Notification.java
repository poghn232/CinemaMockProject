package com.example.superapp.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications", indexes = {
        @Index(name = "idx_notification_profile", columnList = "profile_id"),
        @Index(name = "idx_notification_read", columnList = "profile_id, is_read")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Người nhận thông báo */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false)
    private Profile profile;

    /** Nội dung thông báo */
    @Column(length = 512)
    private String message;

    /** Thời điểm tạo */
    @Column(nullable = false)
    private LocalDateTime createdAt;

    /** Đã đọc chưa */
    @Column(nullable = false)
    private boolean isRead;

    // ── Thông tin phim liên quan ──────────────────────────────────────────

    @Column(nullable = false)
    private Long contentId;

    /** "movie" | "tv" */
    @Column(nullable = false, length = 32)
    private String contentType;

    @Column(length = 256)
    private String contentTitle;

    @Column(length = 512)
    private String posterUrl;

    @Column(nullable = false)
    private String messageKey;

    @Column(columnDefinition = "TEXT")
    private String messageParams; // JSON string

    @Column(name = "icon_url")
    private String iconUrl;

    // ── Loại sự kiện ─────────────────────────────────────────────────────

    /**
     * NEW_MOVIE   – phim mới được thêm vào hệ thống
     * NEW_TRAILER – trailer vừa được cập nhật
     * NEW_SOURCE  – source phim vừa được cập nhật
     */
    @Column(nullable = false, length = 32)
    private String eventType;

    @Column
    private Long episodeId; // null nếu không phải episode notification

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}