package com.example.superapp.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ReviewDto {

    private Long reviewId;
    private long profileId;
    private String profileName;
    private String comment;
    private LocalDateTime createdDate;
}
