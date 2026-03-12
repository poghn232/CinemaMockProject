package com.example.superapp.repository;

import com.example.superapp.entity.WatchHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface WatchHistoryRepository extends JpaRepository<WatchHistory, Long> {

    List<WatchHistory> findByUser_UserIdOrderByWatchedAtDesc(Long userId);

    // Sửa 'MovieId' thành 'Id' (hoặc đúng tên trường ID trong class Movie)
    Optional<WatchHistory> findByUser_UserIdAndMovie_Id(Long userId, Long movieId);

    // Sửa 'EpisodeId' thành 'Id' (hoặc đúng tên trường ID trong class Episode)
    Optional<WatchHistory> findByUser_UserIdAndEpisode_Id(Long userId, Long episodeId);
}
