package com.example.superapp.repository;

import com.example.superapp.entity.WatchHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface WatchHistoryRepository extends JpaRepository<WatchHistory, Long> {

    List<WatchHistory> findAllByProfile_ProfileIdOrderByWatchedAtDesc(Long profileId);

    Optional<WatchHistory> findByProfile_ProfileIdAndMovie_Id(Long profileId, Long movieId);

    Optional<WatchHistory> findByProfile_ProfileIdAndEpisode_Id(Long profileId, Long episodeId);
}
