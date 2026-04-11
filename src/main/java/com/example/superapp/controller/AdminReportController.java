package com.example.superapp.controller;

import com.example.superapp.dto.ReportAdminDto;
import com.example.superapp.entity.AdminLogs;
import com.example.superapp.entity.Profile;
import com.example.superapp.entity.Report;
import com.example.superapp.entity.Review;
import com.example.superapp.entity.User;
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
    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final EmailService emailService;
    private final AdminLogsService adminLogsService;

    @GetMapping
    public List<ReportAdminDto> listAll() {
        return reportRepository.findAll()
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

        // hide all reviews by the reported profile and disable commenting on that profile
        Profile reportedProfile = r.getReview() != null && r.getReview().getProfile() != null ? r.getReview().getProfile() : null;
        if (reportedProfile != null) {
            // hide all reviews from this profile
            var reviews = reviewRepository.findByProfile_ProfileId(reportedProfile.getProfileId());
            for (Review rv : reviews) {
                rv.setHidden(true);
            }
            reviewRepository.saveAll(reviews);
            adminLogsService.saveLog(new AdminLogs(reportedProfile.getProfileName() + " reviews hidden due to approved report"));

            // disable commenting on this profile
            reportedProfile.setCommentDisabled(true);
            profileRepository.save(reportedProfile);
            adminLogsService.saveLog(new AdminLogs(reportedProfile.getProfileName() + " is disabled from commenting due to approved report"));

            // send email to the user who owns this profile
            User reported = reportedProfile.getUser();
            if (reported != null) {
                String to = reported.getEmail();
                String subject = "Your comment has been removed";
                String html = "<div>Dear " + reported.getUsername() + ",<br><br>" +
                        "Your comment has been removed by admin for the following reason:<br><i>" + req.reason + "</i><br><br>" +
                        "Your profile '" + reportedProfile.getProfileName() + "' is temporarily restricted from commenting.<br><br>Regards,<br>MovieZone Admin</div>";
                try {
                    emailService.sendCustomHtml(to, subject, html);
                } catch (Exception e) {
                    // log and continue
                }
            }
        }

        reportRepository.save(r);
        adminLogsService.saveLog(new AdminLogs(r + " is approved"));

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

        // send email to reporter's user
        Profile reporterProfile = r.getReporter();
        if (reporterProfile != null) {
            User reporterUser = reporterProfile.getUser();
            if (reporterUser != null) {
                String to = reporterUser.getEmail();
                String subject = "Your report has been rejected";
                String html = "<div>Dear " + reporterUser.getUsername() + ",<br><br>" +
                        "Your report about a comment was reviewed and rejected by admin for the following reason:<br><i>" + req.reason + "</i><br><br>Regards,<br>MovieZone Admin</div>";
                try {
                    emailService.sendCustomHtml(to, subject, html);
                } catch (Exception e) {
                    // log and continue
                }
            }
        }

        return ResponseEntity.ok().build();
    }

    private ReportAdminDto toDto(Report r) {
        ReportAdminDto d = new ReportAdminDto();
        d.id = r.getId();
        d.reviewId = r.getReview() != null ? r.getReview().getReviewId() : null;
        d.reportedUsername = r.getReview() != null && r.getReview().getProfile() != null ? r.getReview().getProfile().getUser().getUsername() : null;
        d.reporterUsername = r.getReporter() != null ? r.getReporter().getUser().getUsername() : null;
        d.reason = r.getReason();
        d.createdAt = r.getCreatedAt();
        d.status = r.getStatus() != null ? r.getStatus().name() : null;
        d.adminReason = r.getAdminReason();
        d.adminActionAt = r.getAdminActionAt();
        d.comment = r.getReview() != null ? r.getReview().getComment() : null;
        d.adminUsername = r.getAdmin() != null ? r.getAdmin().getUsername() : null;
        return d;
    }

    public static class AdminActionRequest {
        public String reason;
    }
}
