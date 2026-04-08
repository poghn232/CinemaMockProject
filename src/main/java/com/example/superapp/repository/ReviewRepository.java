package com.example.superapp.repository;

import com.example.superapp.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByMovieId(Long movieId);
    List<Review> findByEpisodeId(Long episodeId);

    Optional<Review> findByMovieIdAndUser_Username(Long movieId, String username);
    Optional<Review> findByEpisodeIdAndUser_Username(Long episodeId, String username);
       // return the most recent review by this user for the movie/episode (used when multiple comments are allowed)
       Optional<Review> findTopByMovieIdAndUser_UsernameOrderByCreatedDateDesc(Long movieId, String username);
       Optional<Review> findTopByEpisodeIdAndUser_UsernameOrderByCreatedDateDesc(Long episodeId, String username);
    
       // rating-specific helpers: find the most recent review that contains a rating (>0)
       Optional<Review> findTopByMovieIdAndUser_UsernameAndRatingGreaterThanOrderByCreatedDateDesc(Long movieId, String username, Integer ratingThreshold);
       Optional<Review> findTopByEpisodeIdAndUser_UsernameAndRatingGreaterThanOrderByCreatedDateDesc(Long episodeId, String username, Integer ratingThreshold);
    List<Review> findByUser_UserId(Long userId);

       @Query("SELECT AVG(r.rating) FROM Review r WHERE r.movie.id = :movieId AND r.rating > 0 AND (r.hidden = false OR r.hidden IS NULL)")
    Double getAverageRatingForMovie(@Param("movieId") Long movieId);

       @Query("SELECT COUNT(r) FROM Review r WHERE r.movie.id = :movieId AND r.rating > 0 AND (r.hidden = false OR r.hidden IS NULL)")
    Long getRatingCountForMovie(@Param("movieId") Long movieId);

       @Query("SELECT AVG(r.rating) FROM Review r WHERE r.episode.id = :episodeId AND r.rating > 0 AND (r.hidden = false OR r.hidden IS NULL)")
    Double getAverageRatingForEpisode(@Param("episodeId") Long episodeId);

       @Query("SELECT COUNT(r) FROM Review r WHERE r.episode.id = :episodeId AND r.rating > 0 AND (r.hidden = false OR r.hidden IS NULL)")
       Long getRatingCountForEpisode(@Param("episodeId") Long episodeId);

       @Query("SELECT AVG(r.rating) FROM Review r WHERE r.episode.season.tvSeries.id = :tvId AND r.rating > 0 AND (r.hidden = false OR r.hidden IS NULL)")
       Double getAverageRatingForTvSeries(@Param("tvId") Long tvId);
       @Query("SELECT COUNT(r) FROM Review r WHERE r.episode.season.tvSeries.id = :tvId AND r.rating > 0 AND (r.hidden = false OR r.hidden IS NULL)")
       Long getRatingCountForTvSeries(@Param("tvId") Long tvId);

    @Query("SELECT r.movie, AVG(r.rating) as avgRating FROM Review r " +
           "WHERE r.movie IS NOT NULL AND (r.hidden = false OR r.hidden IS NULL) " +
           "GROUP BY r.movie ORDER BY avgRating DESC")
    List<Object[]> findTopMoviesByRating(org.springframework.data.domain.Pageable pageable);

    @Query("SELECT r.episode.season.tvSeries, AVG(r.rating) as avgRating FROM Review r " +
           "WHERE r.episode IS NOT NULL AND (r.hidden = false OR r.hidden IS NULL) " +
           "GROUP BY r.episode.season.tvSeries ORDER BY avgRating DESC")
    List<Object[]> findTopTvSeriesByRating(org.springframework.data.domain.Pageable pageable);
}
