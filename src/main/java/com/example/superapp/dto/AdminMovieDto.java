package com.example.superapp.dto;

public class AdminMovieDto {

    private Long id;
    private String title;
    private String type; // "movie" | "tv"
    private boolean published;
    private boolean active;

    public AdminMovieDto() {
    }

    public AdminMovieDto(Long id, String title, String type, boolean published, boolean active) {
        this.id = id;
        this.title = title;
        this.type = type;
        this.published = published;
        this.active = active;
    }

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

    public boolean isPublished() {
        return published;
    }

    public void setPublished(boolean published) {
        this.published = published;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}

