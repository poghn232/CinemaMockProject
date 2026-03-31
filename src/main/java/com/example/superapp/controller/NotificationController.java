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
    public ResponseEntity<?> list(Authentication auth) {
        if (auth == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }
        return ResponseEntity.ok(notificationService.getNotifications(auth.getName()));
    }

    @GetMapping("/unread")
    public ResponseEntity<?> unreadCount(Authentication auth) {
        if (auth == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }
        long count = notificationService.countUnread(auth.getName());
        return ResponseEntity.ok(Map.of("count", count));
    }

    @PutMapping("/read-all")
    public ResponseEntity<Void> markAllRead(Authentication auth) {
        notificationService.markAllRead(auth.getName());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<Void> markOneRead(@PathVariable Long id, Authentication auth) {
        notificationService.markOneRead(auth.getName(), id);
        return ResponseEntity.noContent().build();
    }
}