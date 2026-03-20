package com.example.superapp.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "reports")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "review_id")
    private Review review;

    @ManyToOne
    @JoinColumn(name = "reporter_id")
    private Profile reporter;

    @Column(columnDefinition = "TEXT")
    private String reason; // reason provided by reporter

    @Column(name = "created_date")
    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    private Status status;

    @ManyToOne
    @JoinColumn(name = "admin_id")
    private User admin; // admin that handled the report

    @Column(columnDefinition = "TEXT")
    private String adminReason; // reason provided by admin when approving/rejecting

    @Column(name = "admin_action_at")
    private LocalDateTime adminActionAt;

    public enum Status {
        PENDING, APPROVED, REJECTED
    }

    @Override
    public String toString() {
        return "Report " + id;
    }
}
