package com.example.superapp.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "tv_series")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
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

    @Column(nullable = false)
    private Boolean active = true;

    @Column(nullable = false)
    private Boolean published = false;

    @Column(nullable = false)
    private Boolean featured = false;

    /**
     * Nguồn trailer/video (YouTube URL, v.v.)
     */
    private String src;

    @OneToMany(mappedBy = "tvSeries", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<TvRegionBlock> regionBlocks = new HashSet<>();
    public Set<TvRegionBlock> getRegionBlocks() {
        return regionBlocks;
    }
    public void setRegionBlocks(Set<TvRegionBlock> regionBlocks) {
        this.regionBlocks = regionBlocks;
    }

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

    @Override
    public String toString() {
        return "TV Series " + name;
    }
}