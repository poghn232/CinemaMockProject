package com.example.superapp.controller;

import com.example.superapp.dto.WatchHistoryDTO;
import com.example.superapp.entity.User;
import com.example.superapp.entity.WatchHistory;
import com.example.superapp.repository.WatchHistoryRepository;
import com.example.superapp.repository.UserRepository;
import com.example.superapp.repository.MovieRepository; // Cần thêm
import com.example.superapp.repository.EpisodeRepository; // Cần thêm
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import com.example.superapp.utils.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
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
public class WatchHistoryController {

    @Value("${tmdb.image-base-url}")
    private String imageBaseUrl;

    @Autowired
    private WatchHistoryRepository watchHistoryRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private MovieRepository movieRepository;     // Đảm bảo tên class này chính xác
    @Autowired
    private EpisodeRepository episodeRepository; // Đảm bảo tên class này chính xác
    @Autowired
    private JwtUtils jwtUtils;

    // Constructor thủ công để đảm bảo tiêm đủ các Repository
    public WatchHistoryController(WatchHistoryRepository watchHistoryRepository,
            UserRepository userRepository,
            MovieRepository movieRepository,
            EpisodeRepository episodeRepository) {
        this.watchHistoryRepository = watchHistoryRepository;
        this.userRepository = userRepository;
        this.movieRepository = movieRepository;
        this.episodeRepository = episodeRepository;
    }

    @GetMapping
    public ResponseEntity<List<WatchHistoryDTO>> getHistory(Authentication auth, jakarta.servlet.http.HttpServletRequest request) {
        User user = userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

    List<WatchHistory> list = watchHistoryRepository.findByUser_UserIdOrderByWatchedAtDesc(user.getUserId());

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
                dto.setEpisodeName(h.getEpisode().getName());
                if (h.getEpisode().getSeason() != null && h.getEpisode().getSeason().getTvSeries() != null) {
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
    public ResponseEntity<?> saveHistory(Authentication auth, @RequestBody WatchHistoryDTO dto) {
        User user = userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Optional<WatchHistory> existing;
        if (dto.getMovieId() != null) {
            existing = watchHistoryRepository.findByUser_UserIdAndMovie_Id(user.getUserId(), dto.getMovieId());
        } else {
            existing = watchHistoryRepository.findByUser_UserIdAndEpisode_Id(user.getUserId(), dto.getEpisodeId());
        }

        WatchHistory history = existing.orElse(new WatchHistory());

        // QUAN TRỌNG: Gán liên kết thực thể nếu là bản ghi mới
        if (history.getId() == null) {
            history.setUser(user);
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

    private String extractRegionFromRequest(jakarta.servlet.http.HttpServletRequest request) {
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
}
