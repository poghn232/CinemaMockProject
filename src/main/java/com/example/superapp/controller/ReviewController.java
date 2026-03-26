//TODO: update review with profiles
package com.example.superapp.controller;

import com.example.superapp.dto.CreateReviewRequest;
import com.example.superapp.dto.ReviewDto;
import com.example.superapp.entity.Profile;
import com.example.superapp.entity.Report;
import com.example.superapp.repository.ProfileRepository;
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
    private final com.example.superapp.repository.ReportRepository reportRepository;
    private final com.example.superapp.repository.UserRepository userRepository;
    private final com.example.superapp.repository.ReviewRepository reviewRepository;
    private final ProfileRepository profileRepository;

    @GetMapping("/api/public/reviews")
    public ResponseEntity<?> listReviews(
        @RequestParam(required = false) Long movieId,
        @RequestParam(required = false) Long episodeId
    ) {
        return ResponseEntity.ok(Map.of("reviews", reviewService.listReviews(movieId, episodeId)));
    }

    @PostMapping("/api/user/reviews")
    public ResponseEntity<ReviewDto> createReview(@RequestBody CreateReviewRequest request) {
        ReviewDto dto = reviewService.createComment(request.getProfileId(), request.getMovieId(), request.getEpisodeId(), request.getComment());
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/api/user/reviews/{reviewId}/report")
    public ResponseEntity<?> reportReview(@PathVariable Long reviewId, @RequestBody(required = false) ReportRequest req) {
        var reviewEntityOpt = reviewRepository.findById(reviewId);
        if (reviewEntityOpt.isEmpty()) return ResponseEntity.badRequest().body(Map.of("message", "Review not found"));

        var reviewEntity = reviewEntityOpt.get();
        //TODO: Refactor this one
        Profile reporter = profileRepository.findByProfileId(req.profileId).orElseThrow(() -> new IllegalArgumentException("Profile Id: " + req.profileId + " is not available"));

        String reasonText = (req != null && req.reason != null) ? req.reason : "";

        Report report = Report.builder()
                              .review(reviewEntity)
                              .reporter(reporter)
                              .reason(reasonText)
                              .createdAt(LocalDateTime.now())
                              .status(Report.Status.PENDING)
                              .build();

        reportRepository.save(report);

        return ResponseEntity.status(201).body(Map.of("message", "Report received", "reportId", report.getId()));
    }

    public static class ReportRequest {
        public String reason;
        public long profileId;
    }
}
