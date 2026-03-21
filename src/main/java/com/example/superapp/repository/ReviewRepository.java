package com.example.superapp.repository;

import com.example.superapp.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByMovieId(Long movieId);
    List<Review> findByEpisodeId(Long episodeId);
    List<Review> findByProfile_ProfileId(Long pId);
}
