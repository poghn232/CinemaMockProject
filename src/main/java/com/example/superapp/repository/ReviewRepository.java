package com.example.superapp.repository;

import com.example.superapp.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByMovieId(Long movieId);
    List<Review> findByEpisodeId(Long episodeId);

    Optional<Review> findByMovieIdAndUser_Username(Long movieId, String username);
    Optional<Review> findByEpisodeIdAndUser_Username(Long episodeId, String username);
    List<Review> findByUser_UserId(Long userId);
}
