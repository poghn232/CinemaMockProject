package com.example.superapp.controller;

import com.example.superapp.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

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

        try {
            reviewService.saveRating(auth.getName(), type, id, rating);
            return ResponseEntity.ok(Map.of("message", "Rating saved successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/api/reviews/status")
    public ResponseEntity<?> getRatingStatus(
            Authentication auth,
            @RequestParam String type,
            @RequestParam Long id
    ) {
        String username = auth != null ? auth.getName() : null;
        try {
            return ResponseEntity.ok(reviewService.getRatingStatus(username, type, id));
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
}
