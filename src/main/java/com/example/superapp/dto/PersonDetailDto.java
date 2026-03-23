package com.example.superapp.dto;

import java.util.List;

public class PersonDetailDto {
    private Long id;
    private String name;
    private String profilePath;
    private String biography;
    private String birthday;
    private List<MovieItemDto> credits;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getProfilePath() { return profilePath; }
    public void setProfilePath(String profilePath) { this.profilePath = profilePath; }
    public String getBiography() { return biography; }
    public void setBiography(String biography) { this.biography = biography; }
    public String getBirthday() { return birthday; }
    public void setBirthday(String birthday) { this.birthday = birthday; }
    public List<MovieItemDto> getCredits() { return credits; }
    public void setCredits(List<MovieItemDto> credits) { this.credits = credits; }
}
