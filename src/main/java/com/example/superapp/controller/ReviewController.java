package com.example.superapp.controller;

import com.example.superapp.entity.Profile;
import com.example.superapp.entity.Report;
import com.example.superapp.entity.Review;
import com.example.superapp.entity.User;
import com.example.superapp.repository.ProfileRepository;
import com.example.superapp.repository.ReportRepository;
import com.example.superapp.repository.ReviewRepository;
import com.example.superapp.repository.UserRepository;
import com.example.superapp.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;
    private final ReviewRepository reviewRepository;
    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;

    @PostMapping("/api/reviews/save")
    public ResponseEntity<?> saveRating(
            Authentication auth,
            @RequestBody Map<String, Object> payload
    ) {
        if (auth == null) {
            return ResponseEntity.status(401).body(Map.of("message", "Login required to rate"));
        }

        String type = (String) payload.get("type");
        Long id = Long.valueOf(payload.get("id").toString());
        Integer rating = Integer.valueOf(payload.get("rating").toString());
        Long profileId = payload.get("profileId") != null ? Long.valueOf(payload.get("profileId").toString()) : null;

        try {
            reviewService.saveRating(auth.getName(), type, id, rating, profileId);
            return ResponseEntity.ok(Map.of("message", "Rating saved successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/api/reviews/status")
    public ResponseEntity<?> getRatingStatus(
            Authentication auth,
            @RequestParam String type,
            @RequestParam Long id,
            @RequestParam(required = false) Long profileId
    ) {
        String username = auth != null ? auth.getName() : null;
        try {
            return ResponseEntity.ok(reviewService.getRatingStatus(username, type, id, profileId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/api/public/reviews")
    public ResponseEntity<?> getPublicReviews(
            @RequestParam(required = false) Long movieId,
            @RequestParam(required = false) Long episodeId
    ) {
        try {
            return ResponseEntity.ok(reviewService.getPublicReviews(movieId, episodeId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/api/user/reviews")
    public ResponseEntity<?> saveComment(
            Authentication auth,
            @RequestBody Map<String, Object> payload
    ) {
        if (auth == null) {
            return ResponseEntity.status(401).body(Map.of("message", "Login required to comment"));
        }
        try {
            return ResponseEntity.ok(reviewService.saveComment(auth.getName(), payload));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/api/user/reviews/{id}/report")
    public ResponseEntity<?> reportReview(
            Authentication auth,
            @PathVariable Long id,
            @RequestBody Map<String, Object> payload
    ) {
        if (auth == null) {
            return ResponseEntity.status(401).body(Map.of("message", "Login required to report"));
        }
        try {
            String reason = payload.get("reason") != null ? payload.get("reason").toString() : "";
            Long profileId = payload.get("profileId") != null ? Long.valueOf(payload.get("profileId").toString()) : null;

            Review review = reviewRepository.findById(id).orElseThrow(() -> new RuntimeException("Review not found"));
            User user = userRepository.findByUsername(auth.getName()).orElseThrow(() -> new RuntimeException("User not found"));

            // Determine profile for the reporter
            Profile reporterProfile;
            if (profileId != null) {
                reporterProfile = profileRepository.findByProfileIdAndUser(profileId, user)
                        .orElseThrow(() -> new RuntimeException("Profile not found"));
            } else {
                reporterProfile = user.getProfiles().isEmpty() ? null : user.getProfiles().get(0);
                if (reporterProfile == null) throw new RuntimeException("User has no profile");
            }

            Report r = Report.builder()
                    .review(review)
                    .reporter(reporterProfile)
                    .reason(reason)
                    .createdAt(LocalDateTime.now())
                    .status(Report.Status.PENDING)
                    .build();
            reportRepository.save(r);
            return ResponseEntity.ok(Map.of("message", "Report submitted"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
