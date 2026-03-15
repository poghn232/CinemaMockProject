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

    // genres and studios are many-to-many defined earlier; add accessors so service can modify them
    public Set<Genre> getGenres() {
        return genres;
    }

    public void setGenres(Set<Genre> genres) {
        this.genres = genres;
    }

    public Set<Studio> getStudios() {
        return studios;
    }

    public void setStudios(Set<Studio> studios) {
        this.studios = studios;
    }

    public Set<TvCredit> getCredits() {
        return credits;
    }

    public void setCredits(Set<TvCredit> credits) {
        this.credits = credits;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOverview() {
        return overview;
    }

    public void setOverview(String overview) {
        this.overview = overview;
    }

    public String getPosterPath() {
        return posterPath;
    }

    public void setPosterPath(String posterPath) {
        this.posterPath = posterPath;
    }

    public String getBackdropPath() {
        return backdropPath;
    }

    public void setBackdropPath(String backdropPath) {
        this.backdropPath = backdropPath;
    }

    public Double getVoteAverage() {
        return voteAverage;
    }

    public void setVoteAverage(Double voteAverage) {
        this.voteAverage = voteAverage;
    }

    public Integer getVoteCount() {
        return voteCount;
    }

    public void setVoteCount(Integer voteCount) {
        this.voteCount = voteCount;
    }

    public LocalDate getFirstAirDate() {
        return firstAirDate;
    }

    public void setFirstAirDate(LocalDate firstAirDate) {
        this.firstAirDate = firstAirDate;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Boolean getPublished() {
        return published;
    }

    public void setPublished(Boolean published) {
        this.published = published;
    }

    public Boolean getFeatured() {
        return featured;
    }

    public void setFeatured(Boolean featured) {
        this.featured = featured;
    }

    public String getSrc() {
        return src;
    }

    public void setSrc(String src) {
        this.src = src;
    }
}