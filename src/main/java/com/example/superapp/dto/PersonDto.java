package com.example.superapp.dto;

public class PersonDto {
    private Long id;
    private String name;
    private String profilePath;
    private int creditsCount;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getProfilePath() { return profilePath; }
    public void setProfilePath(String profilePath) { this.profilePath = profilePath; }

    public int getCreditsCount() { return creditsCount; }
    public void setCreditsCount(int creditsCount) { this.creditsCount = creditsCount; }
}
