package com.example.superapp.controller;

import com.example.superapp.dto.WatchHistoryDTO;
import com.example.superapp.entity.Profile;
import com.example.superapp.entity.User;
import com.example.superapp.entity.WatchHistory;
import com.example.superapp.repository.WatchHistoryRepository;
import com.example.superapp.repository.UserRepository;
import com.example.superapp.repository.ProfileRepository;
import com.example.superapp.repository.MovieRepository;
import com.example.superapp.repository.EpisodeRepository;
import com.example.superapp.service.AchievementService;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import com.example.superapp.utils.JwtUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/user/history")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class WatchHistoryController {

    private static final Logger log = LoggerFactory.getLogger(WatchHistoryController.class);

    @Value("${tmdb.image-base-url}")
    private String imageBaseUrl;

    private final WatchHistoryRepository watchHistoryRepository;
    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final MovieRepository movieRepository;
    private final EpisodeRepository episodeRepository;
    private final JwtUtils jwtUtils;
    private final AchievementService achievementService;

    @GetMapping
    @Transactional
    public ResponseEntity<List<WatchHistoryDTO>> getHistory(
            Authentication auth,
            @RequestParam(value = "profileId", required = false) Long profileId,
            jakarta.servlet.http.HttpServletRequest request) {

        // Verify profile belongs to user
        User user = userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Fallback to first profile if not provided
        if (profileId == null) {
            if (user.getProfiles() == null || user.getProfiles().isEmpty()) {
                throw new RuntimeException("User has no profile");
            }
            profileId = user.getProfiles().get(0).getProfileId();
        }

        Profile profile = profileRepository.findByProfileIdAndUser(profileId, user)
                .orElseThrow(() -> new RuntimeException("Profile not found"));

        List<WatchHistory> list = watchHistoryRepository.findAllByProfile_ProfileIdOrderByWatchedAtDesc(profile.getProfileId());

        String userRegion = extractRegionFromRequest(request);

        List<WatchHistoryDTO> dtos = list.stream().map(h -> {
            WatchHistoryDTO dto = new WatchHistoryDTO();
            dto.setId(h.getId());
            dto.setProgressSec(h.getProgressSec());
            dto.setDurationSec(h.getDurationSec());
            dto.setWatchedAt(h.getWatchedAt());

            if (h.getMovie() != null) {
                dto.setMovieId(h.getMovie().getId());
                dto.setMovieTitle(h.getMovie().getTitle());
                String p = h.getMovie().getPosterPath();
                if (p != null && !p.isBlank()) {
                    dto.setPosterPath(p.startsWith("http") ? p : imageBaseUrl + p);
                }

                if (userRegion != null && !userRegion.isBlank()) {
                    String rr = userRegion.trim().toUpperCase();
                    var blocks = h.getMovie().getRegionBlocks();
                    if (blocks != null && !blocks.isEmpty()) {
                        boolean blocked = blocks.stream()
                                .map(b -> b.getRegionCode())
                                .filter(code -> code != null && !code.isBlank())
                                .anyMatch(code -> code.trim().equalsIgnoreCase(rr));
                        dto.setBlocked(blocked);
                    }
                }

            } else if (h.getEpisode() != null) {
                dto.setEpisodeId(h.getEpisode().getId());
                dto.setEpisodeNumber(h.getEpisode().getEpisodeNumber());
                dto.setEpisodeName(h.getEpisode().getName());
                if (h.getEpisode().getSeason() != null) {
                    dto.setSeasonNumber(h.getEpisode().getSeason().getSeasonNumber());
                }
                if (h.getEpisode().getSeason() != null && h.getEpisode().getSeason().getTvSeries() != null) {
                    dto.setTvSeriesId(h.getEpisode().getSeason().getTvSeries().getId());
                    dto.setTvSeriesName(h.getEpisode().getSeason().getTvSeries().getName());
                    String tp = h.getEpisode().getSeason().getTvSeries().getPosterPath();
                    if (tp != null && !tp.isBlank()) {
                        dto.setPosterPath(tp.startsWith("http") ? tp : imageBaseUrl + tp);
                    }

                    if (userRegion != null && !userRegion.isBlank()) {
                        String rr = userRegion.trim().toUpperCase();
                        var blocks = h.getEpisode().getSeason().getTvSeries().getRegionBlocks();
                        if (blocks != null && !blocks.isEmpty()) {
                            boolean blocked = blocks.stream()
                                    .map(b -> b.getRegionCode())
                                    .filter(code -> code != null && !code.isBlank())
                                    .anyMatch(code -> code.trim().equalsIgnoreCase(rr));
                            dto.setBlocked(blocked);
                        }
                    }
                }
            }
            return dto;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    @PostMapping("/save")
    @Transactional
    public ResponseEntity<?> saveHistory(
            Authentication auth,
            @RequestBody WatchHistoryDTO dto,
            @RequestHeader(value = "X-Profile-Id", required = false) Long headerProfileId) {

        User user = userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Determine profileId: prefer header, fallback to first profile
        Long profileId = headerProfileId;
        if (profileId == null) {
            Profile firstProfile = user.getProfiles().isEmpty() ? null : user.getProfiles().get(0);
            if (firstProfile == null) throw new RuntimeException("User has no profile");
            profileId = firstProfile.getProfileId();
        }

        Profile profile = profileRepository.findByProfileIdAndUser(profileId, user)
                .orElseThrow(() -> new RuntimeException("Profile not found"));

        Optional<WatchHistory> existing;
        if (dto.getMovieId() != null) {
            existing = watchHistoryRepository.findByProfile_ProfileIdAndMovie_Id(profile.getProfileId(), dto.getMovieId());
        } else {
            existing = watchHistoryRepository.findByProfile_ProfileIdAndEpisode_Id(profile.getProfileId(), dto.getEpisodeId());
        }

        WatchHistory history = existing.orElse(new WatchHistory());

        if (history.getId() == null) {
            history.setProfile(profile);
            if (dto.getMovieId() != null) {
                history.setMovie(movieRepository.findById(dto.getMovieId()).orElse(null));
            } else if (dto.getEpisodeId() != null) {
                history.setEpisode(episodeRepository.findById(dto.getEpisodeId()).orElse(null));
            }
        }

        history.setProgressSec(dto.getProgressSec());
        history.setDurationSec(dto.getDurationSec());
        history.setWatchedAt(LocalDateTime.now());

        watchHistoryRepository.saveAndFlush(history);

        // Achievement check runs in a separate context — failure should NOT rollback watch history
        final User checkedUser = user;
        try {
            achievementService.checkWatchAchievements(profile);
        } catch (Exception e) {
            log.warn("Achievement check failed (non-fatal): {}", e.getMessage());
        }
        return ResponseEntity.ok(Map.of("message", "History saved"));
    }

    private String extractRegionFromRequest(jakarta.servlet.http.HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        try {
            String token = authHeader.substring(7);
            String region = jwtUtils.extractRegion(token);
            if (region == null || region.isBlank()) {
                return null;
            }
            return region.trim().toUpperCase();
        } catch (Exception e) {
            return null;
        }
    }
}
