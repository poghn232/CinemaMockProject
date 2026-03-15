package com.example.superapp.service;

import com.example.superapp.dto.WishlistItemDto;
import com.example.superapp.dto.WishlistToggleRequest;
import com.example.superapp.dto.WishlistToggleResponse;
import com.example.superapp.entity.User;
import com.example.superapp.entity.Wishlist;
import com.example.superapp.repository.MovieRepository;
import com.example.superapp.repository.TvSeriesRepository;
import com.example.superapp.repository.UserRepository;
import com.example.superapp.repository.WishlistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final UserRepository userRepository;
    private final MovieRepository movieRepository;
    private final TvSeriesRepository tvSeriesRepository;

    private User getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
    }

    public List<WishlistItemDto> getWishlist(String username) {
        User user = getUser(username);
        return wishlistRepository.findByUserOrderByAddedAtDesc(user)
                .stream()
                .map(w -> {
                    String title = null;
                    String posterUrl = null;
                    Double rating = null;
                    Integer year = null;

                    if ("movie".equals(w.getContentType())) {
                        var movie = movieRepository.findById(w.getContentId()).orElse(null);
                        if (movie != null) {
                            title     = movie.getTitle();
                            posterUrl = movie.getPosterPath() != null
                                    ? "https://image.tmdb.org/t/p/w500" + movie.getPosterPath()
                                    : null;
                            rating    = movie.getVoteAverage();
                            year      = movie.getReleaseDate() != null
                                    ? movie.getReleaseDate().getYear() : null;
                        }
                    } else {
                        var tv = tvSeriesRepository.findById(w.getContentId()).orElse(null);
                        if (tv != null) {
                            title     = tv.getName();
                            posterUrl = tv.getPosterPath() != null
                                    ? "https://image.tmdb.org/t/p/w500" + tv.getPosterPath()
                                    : null;
                            rating    = tv.getVoteAverage();
                            year      = tv.getFirstAirDate() != null
                                    ? tv.getFirstAirDate().getYear() : null;
                        }
                    }

                    return new WishlistItemDto(
                            w.getId(), w.getContentId(), w.getContentType(), w.getAddedAt(),
                            title, posterUrl, rating, year
                    );
                })
                .collect(Collectors.toList());
    }

    public WishlistToggleResponse check(String username, Long contentId, String contentType) {
        User user = getUser(username);
        boolean exists = wishlistRepository.existsByUserAndContentIdAndContentType(user, contentId, contentType);
        return new WishlistToggleResponse(exists, exists ? "In wishlist" : "Not in wishlist");
    }

    @Transactional
    public WishlistToggleResponse toggle(String username, WishlistToggleRequest req) {
        User user = getUser(username);
        boolean exists = wishlistRepository.existsByUserAndContentIdAndContentType(
                user, req.contentId(), req.contentType());

        if (exists) {
            wishlistRepository.deleteByUserAndContentIdAndContentType(
                    user, req.contentId(), req.contentType());
            return new WishlistToggleResponse(false, "Removed from wishlist");
        } else {
            wishlistRepository.save(Wishlist.builder()
                    .user(user)
                    .contentId(req.contentId())
                    .contentType(req.contentType())
                    .build());
            return new WishlistToggleResponse(true, "Added to wishlist");
        }
    }
}