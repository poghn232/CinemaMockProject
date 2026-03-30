package com.example.superapp.service;

import com.example.superapp.controller.WishlistController;
import com.example.superapp.dto.WishlistItemDto;
import com.example.superapp.dto.WishlistToggleRequest;
import com.example.superapp.dto.WishlistToggleResponse;
import com.example.superapp.entity.Profile;
import com.example.superapp.entity.User;
import com.example.superapp.entity.Wishlist;
import com.example.superapp.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final MovieRepository movieRepository;
    private final TvSeriesRepository tvSeriesRepository;
    private final ProfileRepository profileRepository;

    public List<WishlistItemDto> getWishlist(long profileId) {
        Profile profile = profileRepository.findByProfileId(profileId).orElseThrow(() -> new IllegalArgumentException("Profile ID: " + profileId + " is not available"));
        return wishlistRepository.findByProfileOrderByAddedAtDesc(profile)
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

    public WishlistToggleResponse check(long profileId, Long contentId, String contentType) {
        Profile profile = profileRepository.findByProfileId(profileId).orElseThrow(() -> new IllegalArgumentException("WishlistToggleResponse - Profile ID: " + profileId + " is not available"));
        boolean exists = wishlistRepository.existsByProfileAndContentIdAndContentType(profile, contentId, contentType);
        return new WishlistToggleResponse(exists, exists ? "In wishlist" : "Not in wishlist");
    }

    @Transactional
    public WishlistToggleResponse toggle(WishlistToggleRequest req) {
        Profile profile = profileRepository.findByProfileId(req.profileId()).orElseThrow(() -> new IllegalArgumentException("WishlistToggleResponse - Profile ID: " + req.profileId() + " is not available"));
        boolean exists = wishlistRepository.existsByProfileAndContentIdAndContentType(profile, req.contentId(), req.contentType());

        if (exists) {
            wishlistRepository.deleteByProfileAndContentIdAndContentType(
                    profile, req.contentId(), req.contentType());
            return new WishlistToggleResponse(false, "Removed from wishlist");
        } else {
            wishlistRepository.save(Wishlist.builder()
                    .profile(profile)
                    .contentId(req.contentId())
                    .contentType(req.contentType())
                    .build());
            return new WishlistToggleResponse(true, "Added to wishlist");
        }
    }
}