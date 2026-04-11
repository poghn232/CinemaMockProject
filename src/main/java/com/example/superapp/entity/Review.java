package com.example.superapp.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Check;

import java.time.LocalDateTime;

@Entity
@Table(name = "reviews")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Check(constraints = "(movie_id is not null and episode_id is null) or (movie_id is null and episode_id is not null)")
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long reviewId;

    @Column(nullable = false)
    private Integer rating; // ví dụ 1 - 10

    @Column(length = 1000)
    private String comment;

    @Column(nullable = false)
    private LocalDateTime createdDate;

    @Column(name = "hidden")
    @Builder.Default
    private Boolean hidden = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false)
    private Profile profile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "episode_id", nullable = true)
    private Episode episode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "movie_id", nullable = true)
    private Movie movie;

    @Override
    public String toString() {
        return "Review " + reviewId;
    }
}
