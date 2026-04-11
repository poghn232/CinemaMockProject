package com.example.superapp.repository;

import com.example.superapp.entity.Notification;
import com.example.superapp.entity.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /** Lấy tất cả notification của profile, mới nhất trước */
    List<Notification> findByProfileOrderByCreatedAtDesc(Profile profile);

    /** Lấy tối đa N notification mới nhất */
    List<Notification> findTop20ByProfileOrderByCreatedAtDesc(Profile profile);

    /** Số notification chưa đọc */
    long countByProfileAndIsReadFalse(Profile profile);

    /** Đánh dấu toàn bộ là đã đọc */
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.profile = :profile AND n.isRead = false")
    void markAllAsRead(@Param("profile") Profile profile);

    /** Đánh dấu một notification đã đọc */
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.id = :id AND n.profile = :profile")
    void markOneAsRead(@Param("id") Long id, @Param("profile") Profile profile);

    /** Kiểm tra xem đã có notification cùng event cho profile chưa (tránh duplicate) */
    boolean existsByProfileAndContentIdAndContentTypeAndEventType(
            Profile profile, Long contentId, String contentType, String eventType);

    boolean existsByProfileAndContentIdAndContentTypeAndEventTypeAndEpisodeId(
            Profile profile, Long contentId, String contentType, String eventType, Long episodeId);
}