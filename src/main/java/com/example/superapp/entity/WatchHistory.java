package com.example.superapp.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;              // QUAN TRỌNG: Phải có dòng này
import lombok.NoArgsConstructor;   // QUAN TRỌNG: Phải có dòng này
import lombok.AllArgsConstructor;  // QUAN TRỌNG: Phải có dòng này
import java.time.LocalDateTime;

@Entity
@Table(name = "watch_history")
@Data
@NoArgsConstructor
@AllArgsConstructor
@org.hibernate.annotations.Check(constraints = "(movie_id IS NOT NULL AND episode_id IS NULL) OR (movie_id IS NULL AND episode_id IS NOT NULL)")
public class WatchHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false)
    private Profile profile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "movie_id")
    private Movie movie;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "episode_id")
    private Episode episode;

    @Column(name = "progress_sec")
    private Integer progressSec;

    @Column(name = "duration_sec")
    private Integer durationSec;

    @Column(name = "watched_at")
    private LocalDateTime watchedAt;
}
