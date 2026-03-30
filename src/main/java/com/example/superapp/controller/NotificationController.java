//TODO: update notification with profiles
package com.example.superapp.controller;

import com.example.superapp.dto.NotificationDto;
import com.example.superapp.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST API cho notification của user đang đăng nhập.
 *
 *  GET  /api/user/notifications          → danh sách 20 notification gần nhất
 *  GET  /api/user/notifications/unread   → { count: N }
 *  PUT  /api/user/notifications/read-all → đánh dấu tất cả đã đọc
 *  PUT  /api/user/notifications/{id}/read → đánh dấu 1 notification đã đọc
 */
@RestController
@RequestMapping("/api/user/notifications")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<List<NotificationDto>> list(@RequestParam long profileId) {
        System.out.println("called list() - NotificationController");
        return ResponseEntity.ok(notificationService.getNotifications(profileId));
    }

    @GetMapping("/unread")
    public ResponseEntity<Map<String, Long>> unreadCount(@RequestParam long profileId) {
        System.out.println("called unread() - NotificationController");
        long count = notificationService.countUnread(profileId);
        return ResponseEntity.ok(Map.of("count", count));
    }

    @PutMapping("/read-all")
    public ResponseEntity<Void> markAllRead(@RequestBody WishlistController.ProfileRequest profileRequest) {
        System.out.println("called readAll() - NotificationController");
        notificationService.markAllRead(profileRequest.profileId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<Void> markOneRead(@PathVariable Long id, @RequestBody WishlistController.ProfileRequest profileRequest) {
        System.out.println("called readOne() - NotificationController");
        notificationService.markOneRead(profileRequest.profileId, id);
        return ResponseEntity.noContent().build();
    }
}