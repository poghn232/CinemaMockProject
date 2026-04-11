package com.example.superapp.controller;

import com.example.superapp.service.NotificationService;
import com.example.superapp.entity.User;
import com.example.superapp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST API cho notification của profile.
 * profileId is optional — falls back to user's first profile if not provided.
 */
@RestController
@RequestMapping("/api/user/notifications")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class NotificationController {

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<?> list(Authentication auth,
                                  @RequestParam(value = "profileId", required = false) Long profileId) {
        if (auth == null) return ResponseEntity.status(401).body("Unauthorized");
        Long pid = resolveProfileId(auth, profileId);
        return ResponseEntity.ok(notificationService.getNotifications(pid));
    }

    @GetMapping("/unread")
    public ResponseEntity<?> unreadCount(Authentication auth,
                                         @RequestParam(value = "profileId", required = false) Long profileId) {
        if (auth == null) return ResponseEntity.status(401).body("Unauthorized");
        Long pid = resolveProfileId(auth, profileId);
        long count = notificationService.countUnread(pid);
        return ResponseEntity.ok(Map.of("count", count));
    }

    @PutMapping("/read-all")
    public ResponseEntity<Void> markAllRead(Authentication auth,
                                            @RequestParam(value = "profileId", required = false) Long profileId) {
        Long pid = resolveProfileId(auth, profileId);
        notificationService.markAllRead(pid);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<Void> markOneRead(@PathVariable Long id, Authentication auth,
                                            @RequestParam(value = "profileId", required = false) Long profileId) {
        Long pid = resolveProfileId(auth, profileId);
        notificationService.markOneRead(pid, id);
        return ResponseEntity.noContent().build();
    }

    private Long resolveProfileId(Authentication auth, Long profileId) {
        if (profileId != null) return profileId;
        User user = userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (user.getProfiles() == null || user.getProfiles().isEmpty()) {
            throw new RuntimeException("User has no profile");
        }
        return user.getProfiles().get(0).getProfileId();
    }
}