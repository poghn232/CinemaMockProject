package com.example.superapp.repository;

import com.example.superapp.entity.WatchHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface WatchHistoryRepository extends JpaRepository<WatchHistory, Long> {

    List<WatchHistory> findAllByProfile_ProfileIdOrderByWatchedAtDesc(Long profileId);

    // Sửa 'MovieId' thành 'Id' (hoặc đúng tên trường ID trong class Movie)
    Optional<WatchHistory> findByProfile_ProfileIdAndMovie_Id(Long profileId, Long movieId);

    // Sửa 'EpisodeId' thành 'Id' (hoặc đúng tên trường ID trong class Episode)
    Optional<WatchHistory> findByProfile_ProfileIdAndEpisode_Id(Long profileId, Long episodeId);
}
