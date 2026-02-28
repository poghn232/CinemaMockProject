package com.example.superapp.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "episodes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Episode {

    @Id
    private Long id;

    private String name;

    @Column(length = 5000)
    private String overview;

    private Integer episodeNumber;
    private LocalDate airDate;

    private Double voteAverage;

    @ManyToOne
    @JoinColumn(name = "season_id")
    private Season season;
}
