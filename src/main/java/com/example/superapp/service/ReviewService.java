package com.example.superapp.service;

import com.example.superapp.entity.Episode;
import com.example.superapp.entity.Movie;
import com.example.superapp.entity.Review;
import com.example.superapp.entity.User;
import com.example.superapp.repository.EpisodeRepository;
import com.example.superapp.repository.MovieRepository;
import com.example.superapp.repository.ReviewRepository;
import com.example.superapp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final MovieRepository movieRepository;
    private final EpisodeRepository episodeRepository;

    @Transactional
    public void saveRating(String username, String type, Long id, Integer rating) {
        if (rating < 1 || rating > 10) {
            throw new IllegalArgumentException("Rating must be between 1 and 10");
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Review review;
        if ("movie".equalsIgnoreCase(type)) {
            review = reviewRepository.findByMovieIdAndUser_Username(id, username)
                    .orElseGet(() -> {
                        Movie movie = movieRepository.findById(id)
                                .orElseThrow(() -> new RuntimeException("Movie not found"));
                        return Review.builder()
                                .user(user)
                                .movie(movie)
                                .createdDate(LocalDateTime.now())
                                .build();
                    });
        } else if ("episode".equalsIgnoreCase(type)) {
            review = reviewRepository.findByEpisodeIdAndUser_Username(id, username)
                    .orElseGet(() -> {
                        Episode episode = episodeRepository.findById(id)
                                .orElseThrow(() -> new RuntimeException("Episode not found"));
                        return Review.builder()
                                .user(user)
                                .episode(episode)
                                .createdDate(LocalDateTime.now())
                                .build();
                    });
        } else {
            throw new IllegalArgumentException("Invalid type: " + type);
        }

        review.setRating(rating);
        review.setCreatedDate(LocalDateTime.now()); // Update timestamp if re-rating
        reviewRepository.save(review);
    }

    public Map<String, Object> getRatingStatus(String username, String type, Long id) {
        boolean hasRated = false;
        Integer userRating = null;

        if (username != null) {
            Optional<Review> existing;
            if ("movie".equalsIgnoreCase(type)) {
                existing = reviewRepository.findByMovieIdAndUser_Username(id, username);
            } else {
                existing = reviewRepository.findByEpisodeIdAndUser_Username(id, username);
            }
            if (existing.isPresent()) {
                hasRated = true;
                userRating = existing.get().getRating();
            }
        }

        Double average;
        Long count;

        if ("movie".equalsIgnoreCase(type)) {
            average = reviewRepository.getAverageRatingForMovie(id);
            count = reviewRepository.getRatingCountForMovie(id);
        } else if ("episode".equalsIgnoreCase(type)) {
            average = reviewRepository.getAverageRatingForEpisode(id);
            count = reviewRepository.getRatingCountForEpisode(id);
        } else if ("tv".equalsIgnoreCase(type)) {
            average = reviewRepository.getAverageRatingForTvSeries(id);
            count = reviewRepository.getRatingCountForTvSeries(id);
        } else {
            throw new IllegalArgumentException("Invalid type: " + type);
        }

        return Map.of(
                "hasRated", hasRated,
                "userRating", userRating != null ? userRating : 0,
                "average", average != null ? average : 0.0,
                "count", count != null ? count : 0
        );
    }

    public Map<String, Object> getPublicReviews(Long movieId, Long episodeId) {
        List<Review> reviews;
        if (episodeId != null) {
            reviews = reviewRepository.findByEpisodeId(episodeId);
        } else if (movieId != null) {
            reviews = reviewRepository.findByMovieId(movieId);
        } else {
            reviews = List.of();
        }

        List<Map<String, Object>> reviewDtos = reviews.stream()
                .filter(r -> !Boolean.TRUE.equals(r.getHidden()) && r.getComment() != null && !r.getComment().trim().isEmpty())
                .sorted((a, b) -> b.getCreatedDate().compareTo(a.getCreatedDate()))
                .map(r -> Map.<String, Object>of(
                        "reviewId", r.getReviewId(),
                        "username", r.getUser().getUsername(),
                        "comment", r.getComment(),
                        "createdDate", r.getCreatedDate(),
                        "rating", r.getRating() != null ? (Object)r.getRating() : (Object)0
                ))
                .toList();

        return Map.of("reviews", reviewDtos);
    }

    @Transactional
    public Map<String, Object> saveComment(String username, Map<String, Object> payload) {
        String commentText = (String) payload.get("comment");
        Long movieId = payload.get("movieId") != null ? Long.valueOf(payload.get("movieId").toString()) : null;
        Long episodeId = payload.get("episodeId") != null ? Long.valueOf(payload.get("episodeId").toString()) : null;

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (Boolean.TRUE.equals(user.getCommentDisabled())) {
            throw new RuntimeException("You are disabled from commenting.");
        }

        Review review;
        if (movieId != null) {
            review = reviewRepository.findByMovieIdAndUser_Username(movieId, username)
                    .orElseGet(() -> {
                        Movie movie = movieRepository.findById(movieId)
                                .orElseThrow(() -> new RuntimeException("Movie not found"));
                        return Review.builder().user(user).movie(movie).build();
                    });
        } else if (episodeId != null) {
            review = reviewRepository.findByEpisodeIdAndUser_Username(episodeId, username)
                    .orElseGet(() -> {
                        Episode episode = episodeRepository.findById(episodeId)
                                .orElseThrow(() -> new RuntimeException("Episode not found"));
                        return Review.builder().user(user).episode(episode).build();
                    });
        } else {
            throw new IllegalArgumentException("Either movieId or episodeId must be provided");
        }

        review.setComment(commentText);
        review.setCreatedDate(LocalDateTime.now());
        if (review.getRating() == null) {
            review.setRating(0); // Default if not rated
        }
        
        Review saved = reviewRepository.save(review);
        return Map.<String, Object>of(
                "reviewId", saved.getReviewId(),
                "username", saved.getUser().getUsername(),
                "comment", saved.getComment(),
                "createdDate", saved.getCreatedDate(),
                "rating", saved.getRating() != null ? (Object)saved.getRating() : (Object)0
        );
    }
}
