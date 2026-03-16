package com.example.superapp.repository;

import com.example.superapp.entity.Notification;
import com.example.superapp.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /** Lấy tất cả notification của user, mới nhất trước */
    List<Notification> findByUserOrderByCreatedAtDesc(User user);

    /** Lấy tối đa N notification mới nhất */
    List<Notification> findTop20ByUserOrderByCreatedAtDesc(User user);

    /** Số notification chưa đọc */
    long countByUserAndIsReadFalse(User user);

    /** Đánh dấu toàn bộ là đã đọc */
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.user = :user AND n.isRead = false")
    void markAllAsRead(@Param("user") User user);

    /** Đánh dấu một notification đã đọc */
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.id = :id AND n.user = :user")
    void markOneAsRead(@Param("id") Long id, @Param("user") User user);

    /** Kiểm tra xem đã có notification cùng event cho user chưa (tránh duplicate) */
    boolean existsByUserAndContentIdAndContentTypeAndEventType(
            User user, Long contentId, String contentType, String eventType);

    boolean existsByUserAndContentIdAndContentTypeAndEventTypeAndEpisodeId(
            User user, Long contentId, String contentType, String eventType, Long episodeId);
}