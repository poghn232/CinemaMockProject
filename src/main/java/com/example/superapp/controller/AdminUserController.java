package com.example.superapp.controller;

import com.example.superapp.dto.UserAdminDto;
import com.example.superapp.entity.AdminLogs;
import com.example.superapp.entity.User;
import com.example.superapp.repository.AdminLogsRepository;
import com.example.superapp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private static final Logger log = LoggerFactory.getLogger(AdminUserController.class);

    private final UserRepository userRepository;
    private final com.example.superapp.repository.ReviewRepository reviewRepository;
    private final AdminLogsRepository adminLogsRepository;

    @GetMapping
    public List<UserAdminDto> all() {
    return userRepository.findAll()
        .stream()
        // only return non-admin users to the admin UI
        .filter(u -> u.getRole() == null || !u.getRole().toUpperCase().contains("ADMIN"))
        .map(u -> new UserAdminDto(u.getUserId(), u.getUsername(), u.getEmail(), u.getRole(), u.getCommentDisabled()))
        .toList();
    }

    @PutMapping("/{id}/enabled")
    public UserAdminDto setEnabled(@PathVariable Long id, @RequestBody Boolean enabled) {
        User u = userRepository.findById(id).orElseThrow(() -> new RuntimeException("User not found"));
    // disallow changing admin accounts
    if (u.getRole() != null && u.getRole().toUpperCase().contains("ADMIN")) {
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot modify admin account");
    }
    // Toggle commenting instead of login
    u.setCommentDisabled(enabled);
        User saved = userRepository.save(u);
        String status = enabled ? "enabled" : "disabled";
        adminLogsRepository.save(new AdminLogs("User" + saved.getUsername() + " is now " + status));
        // If we re-enable commenting for this user, unhide their previously hidden reviews
        if (Boolean.FALSE.equals(saved.getCommentDisabled())) {
            try {
                var reviews = reviewRepository.findByUser_UserId(saved.getUserId());
                for (var r : reviews) {
                    if (Boolean.TRUE.equals(r.getHidden())) {
                        r.setHidden(false);
                    }
                }
                reviewRepository.saveAll(reviews);
            } catch (Exception ex) {
                log.warn("Failed to unhide reviews for user {}: {}", saved.getUsername(), ex.getMessage());
            }
        }
        log.info("User {} saved commentDisabled={}", saved.getUsername(), saved.getCommentDisabled());
        return new UserAdminDto(saved.getUserId(), saved.getUsername(), saved.getEmail(), saved.getRole(), saved.getCommentDisabled());
    }
}
