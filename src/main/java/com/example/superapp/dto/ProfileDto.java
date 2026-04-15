package com.example.superapp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

@Data
@AllArgsConstructor
public class ProfileDto implements Serializable {
    private long profileId;
    private String profileName;
    private String username;
    private boolean locked;

    // Backward-compatible constructor (defaults locked=false)
    public ProfileDto(long profileId, String profileName, String username) {
        this(profileId, profileName, username, false);
    }
}
