package com.example.superapp.controller;

import com.example.superapp.dto.ProfileAdminDto;
import com.example.superapp.entity.AdminLogs;
import com.example.superapp.entity.Profile;
import com.example.superapp.repository.AdminLogsRepository;
import com.example.superapp.repository.ProfileRepository;
import com.example.superapp.repository.ReviewRepository;
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
    private final ReviewRepository reviewRepository;
    private final AdminLogsRepository adminLogsRepository;
    private final ProfileRepository profileRepository;

    @GetMapping
    public List<ProfileAdminDto> all() {
    return profileRepository.findAll()
        .stream()
        // only return non-admin users to the admin UI
        .filter(p -> p.getUser().getRole() == null || !p.getUser().getRole().toUpperCase().contains("ADMIN"))
        .map(p -> new ProfileAdminDto(p.getUser().getUserId(), p.getProfileName(), p.getUser().getEmail(), p.getUser().getRole(), p.getCommentDisabled()))
        .toList();
    }

    @PutMapping("/{id}/enabled")
    public ProfileAdminDto setEnabled(@PathVariable Long id, @RequestBody Boolean enabled) {
        Profile p = profileRepository.findById(id).orElseThrow(() -> new RuntimeException("User not found"));
    // disallow changing admin accounts
    if (p.getUser().getRole() != null && p.getUser().getRole().toUpperCase().contains("ADMIN")) {
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot modify admin account");
    }
    // Toggle commenting instead of login
    p.setCommentDisabled(enabled);
        Profile savedProfile = profileRepository.save(p);
        String status = enabled ? "disabled" : "enabled";
        adminLogsRepository.save(new AdminLogs("User" + savedProfile.getProfileName() + " is now " + status));
        // If we re-enable commenting for this user, unhide their previously hidden reviews
        if (Boolean.FALSE.equals(savedProfile.getCommentDisabled())) {
            try {
                var reviews = reviewRepository.findByProfile_ProfileId(savedProfile.getProfileId());
                for (var r : reviews) {
                    if (Boolean.TRUE.equals(r.getHidden())) {
                        r.setHidden(false);
                    }
                }
                reviewRepository.saveAll(reviews);
            } catch (Exception ex) {
                log.warn("Failed to unhide reviews for user {}: {}", savedProfile.getProfileName(), ex.getMessage());
            }
        }
        log.info("User {} saved commentDisabled={}", savedProfile.getProfileName(), savedProfile.getCommentDisabled());
        return new ProfileAdminDto(savedProfile.getProfileId(), savedProfile.getProfileName(), savedProfile.getUser().getEmail(), savedProfile.getUser().getRole(), savedProfile.getCommentDisabled());
    }
}
