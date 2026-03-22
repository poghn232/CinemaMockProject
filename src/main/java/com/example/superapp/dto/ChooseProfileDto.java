package com.example.superapp.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class ChooseProfileDto {
    private long profileId;
    private String profileName;
}
