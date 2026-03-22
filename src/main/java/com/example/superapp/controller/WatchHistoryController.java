package com.example.superapp.controller;

import com.example.superapp.dto.WatchHistoryDTO;
import com.example.superapp.entity.Profile;
import com.example.superapp.entity.TvSeries;
import com.example.superapp.entity.User;
import com.example.superapp.entity.WatchHistory;
import com.example.superapp.repository.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/user/history")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class WatchHistoryController {

    @Value("${tmdb.image-base-url}")
    private String imageBaseUrl;
    private final ProfileRepository profileRepository;
    private final WatchHistoryRepository watchHistoryRepository;
    private final UserRepository userRepository;
    private final MovieRepository movieRepository;     // Đảm bảo tên class này chính xác
    private final EpisodeRepository episodeRepository; // Đảm bảo tên class này chính xác


    @GetMapping
    public ResponseEntity<List<WatchHistoryDTO>> getHistory(Authentication auth, HttpServletRequest request) {
        long profileId = Long.parseLong(request.getHeader("X-Profile-Id"));
        User currentUser = userRepository.findByUsername(auth.getName())
                                       .orElseThrow(() -> new RuntimeException("User not found"));
        Profile profile = profileRepository.findByProfileIdAndUser(profileId, currentUser).orElseThrow(() -> new IllegalArgumentException("Cannot find profile with id: " + profileId));

        List<WatchHistory> list = profile.getWatchHistories();

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
            } else if (h.getEpisode() != null) {
                dto.setEpisodeId(h.getEpisode().getId());
                dto.setEpisodeName(h.getEpisode().getName());
                TvSeries tvSeries = h.getEpisode().getSeason().getTvSeries();
                if (h.getEpisode().getSeason() != null && tvSeries != null) {
                    dto.setTvSeriesName(tvSeries.getName());
                    String tp = tvSeries.getPosterPath();
                    if (tp != null && !tp.isBlank()) {
                        dto.setPosterPath(tp.startsWith("http") ? tp : imageBaseUrl + tp);
                    }
                }
            }
            return dto;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    @PostMapping("/save")
    public ResponseEntity<?> saveHistory(Authentication auth,
                                         @RequestBody WatchHistoryDTO dto,
                                         HttpServletRequest request) {
        User user = userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        long id = Long.parseLong(request.getHeader("X-Profile-Id"));

        Profile profile = profileRepository.findByProfileIdAndUser(id,user)
                                           .orElseThrow(
                                               () -> new IllegalArgumentException("Cannot find profile with id: " + id)
                                           );

        Optional<WatchHistory> existing;
        if (dto.getMovieId() != null) {
            existing = watchHistoryRepository.findByProfile_ProfileIdAndMovie_Id(profile.getProfileId(), dto.getMovieId());
        } else {
            existing = watchHistoryRepository.findByProfile_ProfileIdAndEpisode_Id(profile.getProfileId(), dto.getEpisodeId());
        }

        WatchHistory history = existing.orElse(new WatchHistory());

        // QUAN TRỌNG: Gán liên kết thực thể nếu là bản ghi mới
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

        watchHistoryRepository.save(history);
        return ResponseEntity.ok(Map.of("message", "History saved"));
    }
}
