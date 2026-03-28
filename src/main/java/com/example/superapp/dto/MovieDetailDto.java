package com.example.superapp.dto;

import java.util.List;

public class MovieDetailDto {

    private Long id;
    private String title;
    private String type; // movie | tv
    private Integer year;
    private Double rating;
    private Integer voteCount;
    private Integer runtime;
    private String overview;
    private String posterUrl;
    private String backdropUrl;
    private String src; // trailer url
    private String srcFilm; //src film
    private String director;
    private String country;
    private String studio;

    public List<String> getVariants() {
        return variants;
    }

    public void setVariants(List<String> variants) {
        this.variants = variants;
    }

    private List<String> variants;
    private java.util.List<CastMemberDto> cast;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public Double getRating() {
        return rating;
    }

    public void setRating(Double rating) {
        this.rating = rating;
    }

    public Integer getVoteCount() {
        return voteCount;
    }

    public void setVoteCount(Integer voteCount) {
        this.voteCount = voteCount;
    }

    public Integer getRuntime() {
        return runtime;
    }

    public void setRuntime(Integer runtime) {
        this.runtime = runtime;
    }

    public String getOverview() {
        return overview;
    }

    public void setOverview(String overview) {
        this.overview = overview;
    }

    public String getPosterUrl() {
        return posterUrl;
    }

    public void setPosterUrl(String posterUrl) {
        this.posterUrl = posterUrl;
    }

    public String getBackdropUrl() {
        return backdropUrl;
    }

    public void setBackdropUrl(String backdropUrl) {
        this.backdropUrl = backdropUrl;
    }

    public String getSrc() {
        return src;
    }

    public void setSrc(String src) {
        this.src = src;
    }

    public String getDirector() {
        return director;
    }

    public void setDirector(String director) {
        this.director = director;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getStudio() {
        return studio;
    }

    public void setStudio(String studio) {
        this.studio = studio;
    }

    public java.util.List<CastMemberDto> getCast() {
        return cast;
    }

    public void setCast(java.util.List<CastMemberDto> cast) {
        this.cast = cast;
    }

    public String getSrcFilm() {
        return srcFilm;
    }

    public void setSrcFilm(String srcFilm) {
        this.srcFilm = srcFilm;
    }
}
