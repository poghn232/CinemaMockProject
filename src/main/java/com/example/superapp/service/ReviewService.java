package com.example.superapp.service;

import com.example.superapp.dto.ReviewDto;
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

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final MovieRepository movieRepository;
    private final EpisodeRepository episodeRepository;

    @Transactional(readOnly = true)
    public List<ReviewDto> listReviews(Long movieId, Long episodeId) {
        return loadReviews(movieId, episodeId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public ReviewDto createComment(String username, Long movieId, Long episodeId, String comment) {
        if ((movieId == null && episodeId == null) || (movieId != null && episodeId != null)) {
            throw new IllegalArgumentException("Either movieId or episodeId must be provided");
        }

        if (comment == null || comment.trim().isEmpty()) {
            throw new IllegalArgumentException("Comment cannot be empty");
        }

        if (comment.length() > 1000) {
            throw new IllegalArgumentException("Comment is too long (max 1000 characters)");
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Review review;
        if (movieId != null) {
            Movie movie = movieRepository.findById(movieId)
                    .orElseThrow(() -> new IllegalArgumentException("Movie not found"));
            Optional<Review> existing = reviewRepository.findByMovieIdAndUser_Username(movieId, username);
            review = existing.orElseGet(Review::new);
            review.setMovie(movie);
        } else {
            Episode episode = episodeRepository.findById(episodeId)
                    .orElseThrow(() -> new IllegalArgumentException("Episode not found"));
            Optional<Review> existing = reviewRepository.findByEpisodeIdAndUser_Username(episodeId, username);
            review = existing.orElseGet(Review::new);
            review.setEpisode(episode);
        }

        review.setUser(user);
        // keep rating field present to satisfy schema; default to 0 for comments
        review.setRating(0);
        review.setComment(comment == null ? null : comment.trim());
        if (review.getReviewId() == null) {
            review.setCreatedDate(LocalDateTime.now());
        }

        return toDto(reviewRepository.save(review));
    }

    @Transactional(readOnly = true)
    public List<Review> loadReviews(Long movieId, Long episodeId) {
        List<Review> reviews;
        if (episodeId != null) {
            reviews = reviewRepository.findByEpisodeId(episodeId);
        } else if (movieId != null) {
            reviews = reviewRepository.findByMovieId(movieId);
        } else {
            return List.of();
        }
        reviews = new java.util.ArrayList<>(reviews); // Make mutable
        reviews.sort(java.util.Comparator.comparing(Review::getCreatedDate, java.util.Comparator.nullsLast(java.util.Comparator.reverseOrder())));
        return reviews;
    }

    private ReviewDto toDto(Review review) {
        ReviewDto dto = new ReviewDto();
        dto.setReviewId(review.getReviewId());
        dto.setUsername(review.getUser() != null ? review.getUser().getUsername() : "Unknown");
        dto.setComment(review.getComment());
        dto.setCreatedDate(review.getCreatedDate() != null ? review.getCreatedDate() : LocalDateTime.now());
        return dto;
    }
}
