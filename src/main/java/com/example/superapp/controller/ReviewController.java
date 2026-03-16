package com.example.superapp.controller;

import com.example.superapp.dto.CreateReviewRequest;
import com.example.superapp.dto.ReviewDto;
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

    @GetMapping("/api/public/reviews")
    public ResponseEntity<?> listReviews(
            @RequestParam(required = false) Long movieId,
            @RequestParam(required = false) Long episodeId
    ) {
        return ResponseEntity.ok(Map.of("reviews", reviewService.listReviews(movieId, episodeId)));
    }

    @PostMapping("/api/user/reviews")
    public ResponseEntity<ReviewDto> createReview(Authentication authentication,
                                                  @RequestBody CreateReviewRequest request) {
        String username = authentication.getName();
        ReviewDto dto = reviewService.createComment(username, request.getMovieId(), request.getEpisodeId(), request.getComment());
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/api/user/reviews/{reviewId}/report")
    public ResponseEntity<?> reportReview(@PathVariable Long reviewId) {
        // This is a lightweight stub. Admin-side review management can be added later.
        return ResponseEntity.ok(Map.of("message", "Report received", "reviewId", reviewId));
    }
}
