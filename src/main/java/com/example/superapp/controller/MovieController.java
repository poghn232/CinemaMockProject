package com.example.superapp.controller;

import com.example.superapp.dto.MoviePageResponse;
import com.example.superapp.entity.SubscriptionStatus;
import com.example.superapp.entity.TvSeries;
import com.example.superapp.entity.User;
import com.example.superapp.entity.VideoAsset;
import com.example.superapp.repository.EpisodeRepository;
import com.example.superapp.repository.SeasonRepository;
import com.example.superapp.repository.SubscriptionRepository;
import com.example.superapp.repository.TvSeriesRepository;
import com.example.superapp.repository.UserRepository;
import com.example.superapp.repository.VideoAssetRepository;
import com.example.superapp.service.R2StorageService;
import com.example.superapp.service.TmdbService;
import com.example.superapp.utils.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/movies")
public class MovieController {

    private final TmdbService tmdbService;
    private final TvSeriesRepository tvSeriesRepository;
    private final SeasonRepository seasonRepository;
    private final EpisodeRepository episodeRepository;
    private final VideoAssetRepository videoAssetRepository;
    private final R2StorageService r2StorageService;
    private final JwtUtils jwtUtils;
    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;

    public MovieController(
            TmdbService tmdbService,
            TvSeriesRepository tvSeriesRepository,
            SeasonRepository seasonRepository,
            EpisodeRepository episodeRepository,
            VideoAssetRepository videoAssetRepository,
            R2StorageService r2StorageService,
            JwtUtils jwtUtils,
            UserRepository userRepository,
            SubscriptionRepository subscriptionRepository
    ) {
        this.tmdbService = tmdbService;
        this.tvSeriesRepository = tvSeriesRepository;
        this.seasonRepository = seasonRepository;
        this.episodeRepository = episodeRepository;
        this.videoAssetRepository = videoAssetRepository;
        this.r2StorageService = r2StorageService;
        this.jwtUtils = jwtUtils;
        this.userRepository = userRepository;
        this.subscriptionRepository = subscriptionRepository;
    }

    @GetMapping("/trending")
    public MoviePageResponse trending(
            @RequestParam(name = "type", defaultValue = "all") String type,
            @RequestParam(name = "page", defaultValue = "1") int page
    ) {
        return tmdbService.getTrending(type, page);
    }

    @GetMapping("/details")
    public Map<String, Object> details(
            @RequestParam("id") long tmdbId,
            @RequestParam(name = "type", defaultValue = "movie") String type
    ) {
        String t = type == null ? "movie" : type.trim().toLowerCase();
        if (t.equals("tv")) {
            return tmdbService.getTvDetails(tmdbId);
        }
        return tmdbService.getMovieDetails(tmdbId);
    }

    @GetMapping("/search")
    public MoviePageResponse search(
            @RequestParam("q") String q,
            @RequestParam(name = "type", defaultValue = "all") String type,
            @RequestParam(name = "page", defaultValue = "1") int page
    ) {
        return tmdbService.search(q, type, page);
    }

    @GetMapping("/tv/{tvId}/season/{season}/episode/{episode}")
    public Map<String, Object> episodeDetails(
            @PathVariable("tvId") long tvId,
            @PathVariable("season") int season,
            @PathVariable("episode") int episode
    ) {
        return tmdbService.getTvEpisodeDetails(tvId, season, episode);
    }

    @GetMapping("/tv/{tvId}/seasons")
    public List<Map<String, Object>> getSeasons(
            @PathVariable("tvId") long tvId,
            HttpServletRequest request
    ) {
        System.out.println("[MovieController] getSeasons tvId=" + tvId);

        TvSeries tv = tvSeriesRepository.findById(tvId).orElse(null);
        if (tv == null) {
            System.out.println("[MovieController] TV not found: " + tvId);
            return Collections.emptyList();
        }

        boolean premiumUser = isPremiumUser(request);

        List<com.example.superapp.entity.Season> seasons = seasonRepository.findByTvSeriesId(tvId);
        System.out.println("[MovieController] Found " + seasons.size() + " seasons");

        return seasons.stream().map(s -> {
            Map<String, Object> m = new java.util.HashMap<>();
            m.put("seasonNumber", s.getSeasonNumber());

            List<Map<String, Object>> eps = episodeRepository.findBySeasonId(s.getId()).stream()
                    .filter(e -> Boolean.TRUE.equals(e.getPublished()))
                    .map(e -> {
                        Map<String, Object> em = new java.util.HashMap<>();
                        em.put("id", e.getId());
                        em.put("episodeNumber", e.getEpisodeNumber());
                        em.put("name", e.getName());
                        em.put("src", e.getSrc());
                        em.put("srcFilm", e.getSrcFilm());
                        em.put("published", e.getPublished());
                        em.put("premiumUser", premiumUser);

                        List<String> variants =
                                r2StorageService.findVariantsWithoutDb("tv_episode", e.getId());

                        java.util.Optional<String> playbackOpt =
                                r2StorageService.findPlaybackUrlWithoutDb("tv_episode", e.getId());

                        if (playbackOpt.isPresent()) {
                            em.put("srcFilm", playbackOpt.get());
                        }

                        em.put("variants", variants);

                        if (premiumUser) {
                            em.put("allowedVariants", variants);
                        } else {
                            em.put("allowedVariants",
                                    variants.contains("v0") ? List.of("v0") : Collections.emptyList());

                            if (em.get("srcFilm") != null && variants.contains("v0")) {
                                em.put("srcFilm", buildVariantPlaylistUrl((String) em.get("srcFilm"), "v0"));
                            }
                        }

                        return em;
                    })
                    .collect(java.util.stream.Collectors.toList());

            m.put("episodes", eps);
            System.out.println("[MovieController] Season " + s.getSeasonNumber() + " has " + eps.size() + " published episodes");
            return m;
        }).collect(java.util.stream.Collectors.toList());
    }

    private boolean isPremiumUser(HttpServletRequest request) {
        if (request == null) {
            return false;
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return false;
        }

        try {
            String token = authHeader.substring(7);
            String username = jwtUtils.extractUsername(token);

            return userRepository.findByUsername(username)
                    .map(user -> subscriptionRepository.existsByUser_UserIdAndStatusAndEndDateAfter(
                            user.getUserId(),
                            SubscriptionStatus.ACTIVE,
                            LocalDateTime.now()
                    ))
                    .orElse(false);
        } catch (Exception e) {
            return false;
        }
    }

    private String buildVariantPlaylistUrl(String masterUrl, String variantName) {
        if (masterUrl == null || masterUrl.isBlank() || variantName == null || variantName.isBlank()) {
            return masterUrl;
        }

        String marker = "/master.m3u8";
        int idx = masterUrl.lastIndexOf(marker);
        if (idx < 0) {
            return masterUrl;
        }

        String base = masterUrl.substring(0, idx);
        return base + "/" + variantName + "/playlist.m3u8";
    }
}