package com.example.superapp.controller;

import com.example.superapp.dto.ReportAdminDto;
import com.example.superapp.entity.Report;
import com.example.superapp.entity.Review;
import com.example.superapp.entity.User;
import com.example.superapp.repository.ReportRepository;
import com.example.superapp.repository.ReviewRepository;
import com.example.superapp.repository.UserRepository;
import com.example.superapp.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/reports")
@RequiredArgsConstructor
public class AdminReportController {

    private final ReportRepository reportRepository;
    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    @GetMapping
    public List<ReportAdminDto> listPending() {
        return reportRepository.findByStatus(Report.Status.PENDING)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<?> approve(@PathVariable Long id, @RequestBody AdminActionRequest req, Authentication auth) {
        Report r = reportRepository.findById(id).orElseThrow(() -> new RuntimeException("Report not found"));
        if (r.getStatus() != Report.Status.PENDING) return ResponseEntity.badRequest().body("Already handled");

        User admin = userRepository.findByUsername(auth.getName()).orElse(null);
        r.setStatus(Report.Status.APPROVED);
        r.setAdmin(admin);
        r.setAdminReason(req.reason);
        r.setAdminActionAt(LocalDateTime.now());

        // hide the review/comment instead of deleting to preserve DB integrity
        Review review = r.getReview();
        if (review != null) {
            review.setHidden(true);
            reviewRepository.save(review);
        }

        // disable reported user from commenting (reuse enabled flag: set to false)
        User reported = r.getReview() != null && r.getReview().getUser() != null ? r.getReview().getUser() : null;
        if (reported != null) {
            // disable commenting only (do not disable login)
            reported.setCommentDisabled(true);
            userRepository.save(reported);
        }

        reportRepository.save(r);

        // send email to reported user
        if (reported != null) {
            String to = reported.getEmail();
            String subject = "Your comment has been removed";
            String html = "<div>Dear " + reported.getUsername() + ",<br><br>" +
                    "Your comment has been removed by admin for the following reason:<br><i>" + req.reason + "</i><br><br>" +
                    "You are temporarily restricted from commenting.<br><br>Regards,<br>MovieZone Admin</div>";
            try {
                emailService.sendCustomHtml(to, subject, html);
            } catch (Exception e) {
                // log and continue
            }
        }

        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<?> reject(@PathVariable Long id, @RequestBody AdminActionRequest req, Authentication auth) {
        Report r = reportRepository.findById(id).orElseThrow(() -> new RuntimeException("Report not found"));
        if (r.getStatus() != Report.Status.PENDING) return ResponseEntity.badRequest().body("Already handled");

        User admin = userRepository.findByUsername(auth.getName()).orElse(null);
        r.setStatus(Report.Status.REJECTED);
        r.setAdmin(admin);
        r.setAdminReason(req.reason);
        r.setAdminActionAt(LocalDateTime.now());
        reportRepository.save(r);

        // send email to reporter with admin reason
        User reporter = r.getReporter();
        if (reporter != null) {
            String to = reporter.getEmail();
            String subject = "Your report has been rejected";
            String html = "<div>Dear " + reporter.getUsername() + ",<br><br>" +
                    "Your report about a comment was reviewed and rejected by admin for the following reason:<br><i>" + req.reason + "</i><br><br>Regards,<br>MovieZone Admin</div>";
            try {
                emailService.sendCustomHtml(to, subject, html);
            } catch (Exception e) {
                // log and continue
            }
        }

        return ResponseEntity.ok().build();
    }

    private ReportAdminDto toDto(Report r) {
        ReportAdminDto d = new ReportAdminDto();
        d.id = r.getId();
        d.reviewId = r.getReview() != null ? r.getReview().getReviewId() : null;
        d.reportedUsername = r.getReview() != null && r.getReview().getUser() != null ? r.getReview().getUser().getUsername() : null;
        d.reporterUsername = r.getReporter() != null ? r.getReporter().getUsername() : null;
        d.reason = r.getReason();
        d.createdAt = r.getCreatedAt();
        d.status = r.getStatus() != null ? r.getStatus().name() : null;
        d.adminReason = r.getAdminReason();
        d.adminActionAt = r.getAdminActionAt();
        return d;
    }

    public static class AdminActionRequest {
        public String reason;
    }
}
