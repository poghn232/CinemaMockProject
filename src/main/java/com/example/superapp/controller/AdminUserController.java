package com.example.superapp.controller;

import com.example.superapp.dto.ProfileAdminDto;
import com.example.superapp.dto.UserAdminDto;
import com.example.superapp.entity.AdminLogs;
import com.example.superapp.entity.Profile;
import com.example.superapp.entity.User;
import com.example.superapp.repository.AdminLogsRepository;
import com.example.superapp.repository.ProfileRepository;
import com.example.superapp.repository.ReviewRepository;
import com.example.superapp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private static final Logger log = LoggerFactory.getLogger(AdminUserController.class);

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final ReviewRepository reviewRepository;
    private final AdminLogsRepository adminLogsRepository;

    @GetMapping
    public List<UserAdminDto> all() {
        return userRepository.findAll()
            .stream()
            .filter(u -> u.getRole() == null || !u.getRole().toUpperCase().contains("ADMIN"))
            .map(u -> {
                boolean isPremium = u.getSubscriptions() != null && u.getSubscriptions().stream().anyMatch(s -> s.getStatus() != null && s.getStatus().name().equals("ACTIVE"));
                // UserAdminDto now expects (userId, username, email, role, premium, commentDisabled)
                return new UserAdminDto(u.getUserId(), u.getUsername(), u.getEmail(), u.getRole(), isPremium, false);
            })
            .toList();
    }

    @GetMapping("/{userId}/profiles")
    public List<ProfileAdminDto> getProfiles(@PathVariable Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return user.getProfiles().stream()
            .map(p -> new ProfileAdminDto(p.getProfileId(), p.getProfileName(),
                user.getEmail(), user.getRole(), p.getCommentDisabled()))
            .toList();
    }

    @PutMapping("/profiles/{profileId}/comment-disabled")
    public ProfileAdminDto setCommentDisabled(@PathVariable Long profileId, @RequestBody Boolean disabled) {
        Profile profile = profileRepository.findByProfileId(profileId)
                .orElseThrow(() -> new RuntimeException("Profile not found"));
        User user = profile.getUser();

        if (user.getRole() != null && user.getRole().toUpperCase().contains("ADMIN")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot modify admin account");
        }

        profile.setCommentDisabled(disabled);
        profileRepository.save(profile);

        String status = disabled ? "disabled" : "enabled";
        adminLogsRepository.save(new AdminLogs("Profile " + profile.getProfileName() + " commenting is now " + status));

        // If we re-enable commenting for this profile, unhide their previously hidden reviews
        if (Boolean.FALSE.equals(profile.getCommentDisabled())) {
            try {
                var reviews = reviewRepository.findByProfile_ProfileId(profile.getProfileId());
                for (var r : reviews) {
                    // only restore reviews that were NOT hidden due to a report
                    if (Boolean.TRUE.equals(r.getHidden()) && !Boolean.TRUE.equals(r.getHiddenByReport())) {
                        r.setHidden(false);
                        // leave hiddenByReport as-is (if null/false)
                    }
                }
                reviewRepository.saveAll(reviews);
            } catch (Exception ex) {
                log.warn("Failed to unhide reviews for profile {}: {}", profile.getProfileName(), ex.getMessage());
            }
        }

        log.info("Profile {} commentDisabled={}", profile.getProfileName(), profile.getCommentDisabled());
    return new ProfileAdminDto(profile.getProfileId(), profile.getProfileName(),
        user.getEmail(), user.getRole(), profile.getCommentDisabled());
    }

    @PutMapping("/{id}/enabled")
    public UserAdminDto setEnabled(@PathVariable Long id, @RequestBody Boolean enabled) {
        User u = userRepository.findById(id).orElseThrow(() -> new RuntimeException("User not found"));
        if (u.getRole() != null && u.getRole().toUpperCase().contains("ADMIN")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot modify admin account");
        }

        // Toggle comment disabled on ALL profiles of this user
        for (Profile p : u.getProfiles()) {
            p.setCommentDisabled(enabled);
        }
        profileRepository.saveAll(u.getProfiles());

        User saved = userRepository.save(u);
        String status = enabled ? "disabled" : "enabled";
        adminLogsRepository.save(new AdminLogs("User " + saved.getUsername() + " commenting is now " + status));

        // If re-enabling, unhide reviews from all profiles
        if (Boolean.FALSE.equals(enabled)) {
            try {
                for (Profile p : saved.getProfiles()) {
                    var reviews = reviewRepository.findByProfile_ProfileId(p.getProfileId());
                    for (var r : reviews) {
                        // only restore reviews that were NOT hidden due to a report
                        if (Boolean.TRUE.equals(r.getHidden()) && !Boolean.TRUE.equals(r.getHiddenByReport())) {
                            r.setHidden(false);
                        }
                    }
                    reviewRepository.saveAll(reviews);
                }
            } catch (Exception ex) {
                log.warn("Failed to unhide reviews for user {}: {}", saved.getUsername(), ex.getMessage());
            }
        }
        log.info("User {} all profiles commentDisabled={}", saved.getUsername(), enabled);
    boolean isPremium = saved.getSubscriptions() != null && saved.getSubscriptions().stream().anyMatch(s -> s.getStatus() != null && s.getStatus().name().equals("ACTIVE"));
    return new UserAdminDto(saved.getUserId(), saved.getUsername(), saved.getEmail(), saved.getRole(), isPremium, enabled);
    }
}
