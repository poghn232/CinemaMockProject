package com.example.superapp.entity;


import jakarta.persistence.*;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "tv_series")
public class TvSeries {

    @Id
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(length = 5000)
    private String overview;

    private String posterPath;
    private String backdropPath;

    private Double voteAverage;
    private Integer voteCount;

    private LocalDate firstAirDate;

    /* ================= RELATIONSHIPS ================= */

    @ManyToMany
    @JoinTable(
        name = "tv_genres",
        joinColumns = @JoinColumn(name = "tv_id"),
        inverseJoinColumns = @JoinColumn(name = "genre_id")
    )
    private Set<Genre> genres = new HashSet<>();

    @ManyToMany
    @JoinTable(
        name = "tv_studios",
        joinColumns = @JoinColumn(name = "tv_id"),
        inverseJoinColumns = @JoinColumn(name = "studio_id")
    )
    private Set<Studio> studios = new HashSet<>();

    @OneToMany(mappedBy = "tvSeries", cascade = CascadeType.ALL)
    private Set<TvCredit> credits = new HashSet<>();

    @OneToMany(mappedBy = "tvSeries", cascade = CascadeType.ALL)
    private Set<Season> seasons = new HashSet<>();
}