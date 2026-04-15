package com.example.superapp.controller;

import com.example.superapp.dto.ProfileDto;
import com.example.superapp.entity.Profile;
import com.example.superapp.entity.Subscription;
import com.example.superapp.entity.SubscriptionStatus;
import com.example.superapp.entity.User;
import com.example.superapp.repository.ProfileRepository;
import com.example.superapp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/user/profiles")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ProfileController {

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;

    @GetMapping
    public ResponseEntity<?> getProfiles(Authentication auth) {
        User user = userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Ensure legacy users or any users without a profile get a default one
        if (user.getProfiles() == null || user.getProfiles().isEmpty()) {
            Profile defaultProfile = Profile.builder()
                    .profileName(user.getUsername())
                    .user(user)
                    .commentDisabled(false)
                    .build();
            profileRepository.save(defaultProfile);
            user.getProfiles().add(defaultProfile);
        }

        List<ProfileDto> profiles = user.getProfiles().stream()
                .map(p -> new ProfileDto(p.getProfileId(), p.getProfileName(), user.getUsername()))
                .toList();

        return ResponseEntity.ok(profiles);
    }

    @PostMapping
    public ResponseEntity<?> createProfile(Authentication auth, @RequestBody Map<String, String> body) {
        User user = userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        String profileName = body.get("profileName");
        if (profileName == null || profileName.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Profile name is required"));
        }

        // Check max profiles based on active subscription
        // Free users: 1 profile only. Premium: from pack's maxProfiles (default 5)
        int maxProfiles = 1;
        LocalDateTime now = LocalDateTime.now();
        for (Subscription sub : user.getSubscriptions()) {
            if (sub.getStatus() == SubscriptionStatus.ACTIVE
                    && sub.getEndDate() != null
                    && sub.getEndDate().isAfter(now)
                    && sub.getPack() != null) {
                Integer packMax = sub.getPack().getMaxProfiles();
                int effective = (packMax != null && packMax > 0) ? packMax : 5;
                if (effective > maxProfiles) {
                    maxProfiles = effective;
                }
            }
        }

        if (user.getProfiles().size() >= maxProfiles) {
            return ResponseEntity.status(409).body(Map.of(
                    "message", "Maximum profiles reached (" + maxProfiles + "). Upgrade your subscription for more profiles."
            ));
        }

        Profile profile = Profile.builder()
                .profileName(profileName)
                .user(user)
                .commentDisabled(false)
                .build();

        profileRepository.save(profile);

        return ResponseEntity.ok(new ProfileDto(profile.getProfileId(), profile.getProfileName(), user.getUsername()));
    }

    @DeleteMapping("/{profileId}")
    public ResponseEntity<?> deleteProfile(Authentication auth, @PathVariable Long profileId) {
        User user = userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Profile profile = profileRepository.findByProfileIdAndUser(profileId, user)
                .orElseThrow(() -> new RuntimeException("Profile not found"));

        // Don't allow deleting the last profile
        if (user.getProfiles().size() <= 1) {
            return ResponseEntity.badRequest().body(Map.of("message", "Cannot delete the last profile"));
        }

        profileRepository.delete(profile);
        return ResponseEntity.ok(Map.of("message", "Profile deleted"));
    }
}
