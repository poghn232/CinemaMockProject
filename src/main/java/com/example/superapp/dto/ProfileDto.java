package com.example.superapp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Data
@AllArgsConstructor
public class ProfileDto implements Serializable {
    private long profileId;
    private String profileName;
    private String username;
}
