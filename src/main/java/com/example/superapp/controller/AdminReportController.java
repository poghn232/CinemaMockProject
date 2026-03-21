package com.example.superapp.controller;

import com.example.superapp.dto.ReportAdminDto;
import com.example.superapp.entity.*;
import com.example.superapp.repository.ProfileRepository;
import com.example.superapp.repository.ReportRepository;
import com.example.superapp.repository.ReviewRepository;
import com.example.superapp.repository.UserRepository;
import com.example.superapp.service.AdminLogsService;
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
    private final ProfileRepository profileRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final AdminLogsService adminLogsService;

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
            adminLogsService.saveLog(new AdminLogs(review + " is reported successfully"));
        }

        // disable reported profile from commenting (reuse enabled flag: set to false)
        Profile reportedProfile = r.getReview() != null && r.getReview().getProfile() != null ? r.getReview().getProfile() : null;
        if (reportedProfile != null) {
            // disable commenting only (do not disable login)
            reportedProfile.setCommentDisabled(true);
            profileRepository.save(reportedProfile);
            adminLogsService.saveLog(new AdminLogs(reportedProfile + " is reported successfully"));
        }

        reportRepository.save(r);
        adminLogsService.saveLog(new AdminLogs(r + " is approved"));

        // send email to reported user
        if (reportedProfile != null) {
            String to = reportedProfile.getUser().getEmail();
            String subject = "Your comment has been removed";
            String html = "<div>Dear " + reportedProfile.getProfileName() + ",<br><br>" +
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
        adminLogsService.saveLog(new AdminLogs(r + " is rejected"));

        // send email to reporter with admin reason
        Profile reporter = r.getReporter();
        if (reporter != null) {
            String to = reporter.getUser().getEmail();
            String subject = "Your report has been rejected";
            String html = "<div>Dear " + reporter.getProfileName() + ",<br><br>" +
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
        d.reportedProfile = r.getReview() != null && r.getReview().getProfile() != null ? r.getReview().getProfile().getProfileName() : null;
        d.reporterProfile = r.getReporter() != null ? r.getReporter().getProfileName() : null;
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
