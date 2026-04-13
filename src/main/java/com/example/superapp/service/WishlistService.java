package com.example.superapp.service;

import com.example.superapp.dto.WishlistItemDto;
import com.example.superapp.dto.WishlistToggleRequest;
import com.example.superapp.dto.WishlistToggleResponse;
import com.example.superapp.entity.Profile;
import com.example.superapp.entity.User;
import com.example.superapp.entity.Wishlist;
import com.example.superapp.repository.*;
import lombok.RequiredArgsConstructor;
import com.example.superapp.utils.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final ProfileRepository profileRepository;
    private final MovieRepository movieRepository;
    private final TvSeriesRepository tvSeriesRepository;
    private final JwtUtils jwtUtils;
    private final AchievementService achievementService;

    private Profile getProfile(Long profileId) {
        return profileRepository.findByProfileId(profileId)
                .orElseThrow(() -> new RuntimeException("Profile not found: " + profileId));
    }

    public List<WishlistItemDto> getWishlist(Long profileId, HttpServletRequest request) {
        Profile profile = getProfile(profileId);
        String userRegion = extractRegionFromRequest(request);
        String rr = userRegion == null ? null : userRegion.trim().toUpperCase();

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

                    Boolean blocked = false;
                    try {
                        if (rr != null) {
                            if ("movie".equals(w.getContentType())) {
                                var movie = movieRepository.findById(w.getContentId()).orElse(null);
                                if (movie != null && movie.getRegionBlocks() != null && !movie.getRegionBlocks().isEmpty()) {
                                    blocked = movie.getRegionBlocks().stream()
                                            .map(b -> b.getRegionCode())
                                            .filter(code -> code != null && !code.isBlank())
                                            .anyMatch(code -> code.trim().equalsIgnoreCase(rr));
                                }
                            } else {
                                var tv = tvSeriesRepository.findById(w.getContentId()).orElse(null);
                                if (tv != null && tv.getRegionBlocks() != null && !tv.getRegionBlocks().isEmpty()) {
                                    blocked = tv.getRegionBlocks().stream()
                                            .map(b -> b.getRegionCode())
                                            .filter(code -> code != null && !code.isBlank())
                                            .anyMatch(code -> code.trim().equalsIgnoreCase(rr));
                                }
                            }
                        }
                    } catch (Exception ignored) {
                    }

                    return new WishlistItemDto(
                            w.getId(), w.getContentId(), w.getContentType(), w.getAddedAt(),
                            title, posterUrl, rating, year, blocked
                    );
                })
                .collect(Collectors.toList());
    }

    private String extractRegionFromRequest(HttpServletRequest request) {
        if (request == null) return null;
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) return null;
        try {
            String token = authHeader.substring(7);
            String region = jwtUtils.extractRegion(token);
            if (region == null || region.isBlank()) return null;
            return region.trim().toUpperCase();
        } catch (Exception e) {
            return null;
        }
    }

    public WishlistToggleResponse check(Long profileId, Long contentId, String contentType) {
        Profile profile = getProfile(profileId);
        boolean exists = wishlistRepository.existsByProfileAndContentIdAndContentType(profile, contentId, contentType);
        return new WishlistToggleResponse(exists, exists ? "In wishlist" : "Not in wishlist");
    }

    @Transactional
    public WishlistToggleResponse toggle(Long profileId, WishlistToggleRequest req) {
        Profile profile = getProfile(profileId);
        User user = profile.getUser();

        boolean exists = wishlistRepository.existsByProfileAndContentIdAndContentType(
                profile, req.contentId(), req.contentType());

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
            achievementService.checkWishlistAchievements(profile);
            return new WishlistToggleResponse(true, "Added to wishlist");
        }
    }
}