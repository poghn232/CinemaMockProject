package com.example.superapp.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateReviewRequest {

    private long profileId;
    private Long movieId;
    private Long episodeId;
    private String comment;
}
