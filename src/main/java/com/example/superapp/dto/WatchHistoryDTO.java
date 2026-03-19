package com.example.superapp.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class WatchHistoryDTO {
    private Long id;
    private LocalDateTime watchedAt;
    private Integer durationSec;
    private Integer progressSec;
    private String posterPath;
    // Whether the content was region-blocked for the requesting user
    private Boolean blocked = false;

    // Dành cho Phim Lẻ
    private Long movieId;
    private String movieTitle;

    // Dành cho Phim Bộ
    private Long episodeId;
    private String tvSeriesName;
    private String episodeName;
}