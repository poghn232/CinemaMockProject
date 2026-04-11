package com.example.superapp.dto;

public class CreateReviewRequest {

    private Long profileId;
    private Long movieId;
    private Long episodeId;
    private String comment;

    public Long getProfileId() { return profileId; }
    public void setProfileId(Long profileId) { this.profileId = profileId; }

    public Long getMovieId() { return movieId; }
    public void setMovieId(Long movieId) { this.movieId = movieId; }

    public Long getEpisodeId() { return episodeId; }
    public void setEpisodeId(Long episodeId) { this.episodeId = episodeId; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
}
