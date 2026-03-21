//TODO: FAMILY ACCOUNT: refactor this one
package com.example.superapp.service;

import com.example.superapp.dto.ReviewDto;
import com.example.superapp.entity.*;
import com.example.superapp.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
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
    private final ProfileRepository profileRepository;

    @Transactional(readOnly = true)
    public List<ReviewDto> listReviews(Long movieId, Long episodeId) {
        return loadReviews(movieId, episodeId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    //TODO: refactor this method
    @Transactional
    public ReviewDto createComment(String profileName, Long movieId, Long episodeId, String comment) {
        if ((movieId == null && episodeId == null) || (movieId != null && episodeId != null)) {
            throw new IllegalArgumentException("Either movieId or episodeId must be provided");
        }

        if (comment == null || comment.trim().isEmpty()) {
            throw new IllegalArgumentException("Comment cannot be empty");
        }

        if (comment.length() > 1000) {
            throw new IllegalArgumentException("Comment is too long (max 1000 characters)");
        }

        Profile profile = profileRepository.findByProfileName(profileName)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Block commenting for users flagged as commentDisabled
        if (Boolean.TRUE.equals(profile.getCommentDisabled())) {
            // friendlier, more natural message returned to clients
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can't post comments right now.");
        }

    // Always create a new Review row so a user can post multiple comments
    Review review = new Review();
    if (movieId != null) {
        Movie movie = movieRepository.findById(movieId)
            .orElseThrow(() -> new IllegalArgumentException("Movie not found"));
        review.setMovie(movie);
    } else {
        Episode episode = episodeRepository.findById(episodeId)
            .orElseThrow(() -> new IllegalArgumentException("Episode not found"));
        review.setEpisode(episode);
    }

    review.setProfile(new Profile());
        // keep rating field present to satisfy schema; default to 0 for comments
        review.setRating(0);
    review.setComment(comment == null ? null : comment.trim());
    // When a user posts or updates a comment, ensure it's visible (clear hidden flag)
    review.setHidden(false);
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
    // Remove hidden reviews so they don't appear to viewers
    reviews.removeIf(r -> Boolean.TRUE.equals(r.getHidden()));
        reviews.sort(java.util.Comparator.comparing(Review::getCreatedDate, java.util.Comparator.nullsLast(java.util.Comparator.reverseOrder())));
        return reviews;
    }

    private ReviewDto toDto(Review review) {
        ReviewDto dto = new ReviewDto();
        dto.setReviewId(review.getReviewId());
        dto.setUsername(review.getProfile().getUser() != null ? review.getProfile().getUser().getUsername() : "Unknown");
        dto.setComment(review.getComment());
        dto.setCreatedDate(review.getCreatedDate() != null ? review.getCreatedDate() : LocalDateTime.now());
        return dto;
    }
}
