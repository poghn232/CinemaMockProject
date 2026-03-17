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
    private final com.example.superapp.repository.ReportRepository reportRepository;
    private final com.example.superapp.repository.UserRepository userRepository;
    private final com.example.superapp.repository.ReviewRepository reviewRepository;

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

    public ResponseEntity<?> reportReview(@PathVariable Long reviewId, @RequestBody(required = false) ReportRequest req, Authentication auth) {
        var reviewEntityOpt = reviewRepository.findById(reviewId);
        if (reviewEntityOpt.isEmpty()) return ResponseEntity.badRequest().body(Map.of("message","Review not found"));

        var reviewEntity = reviewEntityOpt.get();
        var reporter = userRepository.findByUsername(auth.getName()).orElse(null);

    String reasonText = (req != null && req.reason != null) ? req.reason : "";

    com.example.superapp.entity.Report report = com.example.superapp.entity.Report.builder()
        .review(reviewEntity)
        .reporter(reporter)
        .reason(reasonText)
        .createdAt(java.time.LocalDateTime.now())
        .status(com.example.superapp.entity.Report.Status.PENDING)
        .build();

        reportRepository.save(report);

    return ResponseEntity.status(201).body(Map.of("message", "Report received", "reportId", report.getId()));
    }

    public static class ReportRequest { public String reason; }
}
