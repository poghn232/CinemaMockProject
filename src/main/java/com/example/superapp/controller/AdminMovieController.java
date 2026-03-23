package com.example.superapp.controller;

import com.example.superapp.dto.AdminMovieDto;
import com.example.superapp.dto.VideoAssetDto;
import com.example.superapp.service.AdminMovieService;
import com.example.superapp.entity.TvSeries;
import com.example.superapp.entity.Season;
import com.example.superapp.entity.Episode;
import com.example.superapp.repository.TvSeriesRepository;
import com.example.superapp.repository.SeasonRepository;
import com.example.superapp.repository.EpisodeRepository;
import com.example.superapp.service.VideoAssetService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/movies")
@RequiredArgsConstructor
public class AdminMovieController {

    private final AdminMovieService adminMovieService;
    private final TvSeriesRepository tvSeriesRepository;
    private final SeasonRepository seasonRepository;
    private final EpisodeRepository episodeRepository;
    private final VideoAssetService videoAssetService;

    @GetMapping
    public List<AdminMovieDto> list(@RequestParam(name = "q", required = false) String query) {
        try {
            System.out.println("[AdminMovieController] listing movies, query='" + query + "'");
            return adminMovieService.listPublished(query);
        } catch (Exception ex) {
            ex.printStackTrace();
            throw ex;
        }
    }

    @PostMapping("/import")
    public AdminMovieDto importMovie(@RequestBody ImportRequest request) {
        AdminMovieDto dto = adminMovieService.importFromTmdb(request.getTmdbId(), request.getType());

        if ("movie".equalsIgnoreCase(request.getType())) {
            videoAssetService.syncExistingPlaybackFromR2("movie", request.getTmdbId());
        }

        return dto;
    }

    @PutMapping("/{type}/{id}/hide")
    public void hide(@PathVariable("type") String type, @PathVariable("id") long id) {
        adminMovieService.hide(id, type);
    }

    @PutMapping("/{type}/{id}/publish")
    public void publish(@PathVariable("type") String type, @PathVariable("id") long id) {
        adminMovieService.publish(id, type);
    }

    @PostMapping("/tv/{tvId}/seasons/{seasonNumber}/episodes/{episodeNumber}/import")
    public AdminMovieDto importEpisode(@PathVariable("tvId") long tvId,
                                       @PathVariable("seasonNumber") int seasonNumber,
                                       @PathVariable("episodeNumber") int episodeNumber) {
        AdminMovieDto dto = adminMovieService.importEpisodeFromTmdb(tvId, seasonNumber, episodeNumber);

        Season season = seasonRepository.findByTvSeriesId(tvId).stream()
                .filter(s -> s.getSeasonNumber() != null && s.getSeasonNumber() == seasonNumber)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Season not found"));

        Episode episode = episodeRepository.findBySeasonId(season.getId()).stream()
                .filter(e -> e.getEpisodeNumber() != null && e.getEpisodeNumber() == episodeNumber)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Episode not found"));

        videoAssetService.syncExistingPlaybackFromR2("tv_episode", episode.getId());

        return dto;
    }

    @GetMapping("/tv/{tvId}/seasons/{seasonNumber}/episodes/existing")
    public java.util.List<Integer> existingEpisodes(@PathVariable("tvId") long tvId,
                                                    @PathVariable("seasonNumber") int seasonNumber) {
        return adminMovieService.getExistingEpisodeNumbers(tvId, seasonNumber);
                                                    }

    @GetMapping("/tv/{tvId}/seasons")
    public java.util.List<java.util.Map<String, Object>> getSeasons(@PathVariable("tvId") long tvId) {
        TvSeries tv = tvSeriesRepository.findById(tvId).orElse(null);
        if (tv == null) return java.util.Collections.emptyList();
        java.util.List<com.example.superapp.entity.Season> seasons = seasonRepository.findByTvSeriesId(tvId);
        return seasons.stream().map(s -> {
            java.util.Map<String, Object> m = new java.util.HashMap<>();
            m.put("seasonNumber", s.getSeasonNumber());
            m.put("episodes", episodeRepository.findBySeasonId(s.getId()).stream().map(e -> {
                java.util.Map<String, Object> em = new java.util.HashMap<>();
                em.put("episodeNumber", e.getEpisodeNumber());
                em.put("name", e.getName());
                em.put("src", e.getSrc());
                em.put("published", e.getPublished());
                return em;
            }).collect(java.util.stream.Collectors.toList()));
            return m;
        }).collect(java.util.stream.Collectors.toList());
    }

    @PutMapping("/tv/{tvId}/seasons/{seasonNumber}/episodes/{episodeNumber}/publish")
    public void toggleEpisodePublish(@PathVariable("tvId") long tvId,
                                     @PathVariable("seasonNumber") int seasonNumber,
                                     @PathVariable("episodeNumber") int episodeNumber) {
        adminMovieService.toggleEpisodePublished(tvId, seasonNumber,episodeNumber);
    }

    @PutMapping("/{type}/{id}/trailer")
    public void setTrailer(@PathVariable("type") String type, @PathVariable("id") long id, @RequestBody TrailerRequest request) {
        if ("movie".equals(type)) {
            adminMovieService.setMovieTrailer(id, request.getSrc());
        } else {
            throw new IllegalArgumentException("Trailer setting only supported for movies");
        }
    }

    // REGION BLOCK endpoints
    @GetMapping("/{type}/{id}/regions")
    public java.util.List<java.util.Map<String, Object>> listRegions(@PathVariable("type") String type, @PathVariable("id") long id) {
        return adminMovieService.listRegionBlocks(type, id);
    }

    @PutMapping("/{type}/{id}/regions/{regionCode}")
    public void toggleRegionBlock(@PathVariable("type") String type, @PathVariable("id") long id, @PathVariable("regionCode") String regionCode) {
        adminMovieService.toggleRegionBlock(type, id, regionCode);
    }

    @PutMapping("/tv/{tvId}/seasons/{seasonNumber}/episodes/{episodeNumber}/trailer")
    public void setEpisodeTrailer(@PathVariable("tvId") long tvId,
                                  @PathVariable("seasonNumber") int seasonNumber,
                                  @PathVariable("episodeNumber") int episodeNumber,
                                  @RequestBody TrailerRequest request) {
        adminMovieService.setEpisodeTrailer(tvId, seasonNumber, episodeNumber, request.getSrc());
    }

    @PostMapping("/{type}/{id}/source")
    public ResponseEntity<VideoAssetDto> uploadSource(@PathVariable("type") String type,
                                                      @PathVariable("id") long id,
                                                      @RequestParam("file") MultipartFile file) {
        String ownerType = resolveOwnerType(type);
        VideoAssetDto dto = videoAssetService.uploadSource(ownerType, id, file);
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/{type}/{id}/source/latest")
    public ResponseEntity<VideoAssetDto> getLatestSource(@PathVariable("type") String type,
                                                         @PathVariable("id") long id) {
        String ownerType = resolveOwnerType(type);
        return ResponseEntity.ok(videoAssetService.getLatestAsset(ownerType, id));
    }

    private String resolveOwnerType(String type) {
        String t = type == null ? "" : type.trim().toLowerCase();
        if ("movie".equals(t)) return "movie";
        if ("tv".equals(t)) return "tv";
        throw new IllegalArgumentException("type must be 'movie' or 'tv'");
    }

    @PostMapping("/tv/{tvId}/seasons/{seasonNumber}/episodes/{episodeNumber}/source")
    public ResponseEntity<VideoAssetDto> uploadEpisodeSource(@PathVariable("tvId") long tvId,
                                                             @PathVariable("seasonNumber") int seasonNumber,
                                                             @PathVariable("episodeNumber") int episodeNumber,
                                                             @RequestParam("file") MultipartFile file) {
        TvSeries tv = tvSeriesRepository.findById(tvId)
                .orElseThrow(() -> new IllegalArgumentException("TV series not found"));

        Season season = seasonRepository.findByTvSeriesId(tv.getId()).stream()
                .filter(s -> s.getSeasonNumber() != null && s.getSeasonNumber() == seasonNumber)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Season not found"));

        Episode episode = episodeRepository.findBySeasonId(season.getId()).stream()
                .filter(e -> e.getEpisodeNumber() != null && e.getEpisodeNumber() == episodeNumber)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Episode not found"));

        VideoAssetDto dto = videoAssetService.uploadSource("tv_episode", episode.getId(), file);
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/tv/{tvId}/seasons/{seasonNumber}/episodes/{episodeNumber}/source/latest")
    public ResponseEntity<VideoAssetDto> getLatestEpisodeSource(@PathVariable("tvId") long tvId,
                                                                @PathVariable("seasonNumber") int seasonNumber,
                                                                @PathVariable("episodeNumber") int episodeNumber) {
        TvSeries tv = tvSeriesRepository.findById(tvId)
                .orElseThrow(() -> new IllegalArgumentException("TV series not found"));

        Season season = seasonRepository.findByTvSeriesId(tv.getId()).stream()
                .filter(s -> s.getSeasonNumber() != null && s.getSeasonNumber() == seasonNumber)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Season not found"));

        Episode episode = episodeRepository.findBySeasonId(season.getId()).stream()
                .filter(e -> e.getEpisodeNumber() != null && e.getEpisodeNumber() == episodeNumber)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Episode not found"));

        return ResponseEntity.ok(videoAssetService.getLatestAsset("tv_episode", episode.getId()));
    }

    @PostMapping("/backfill/src-film")
    public ResponseEntity<Map<String, Object>> backfillSrcFilm() {
        int moviesUpdated = videoAssetService.backfillMovieSrcFilmFromR2();
        int episodesUpdated = videoAssetService.backfillEpisodeSrcFilmFromR2();

        Map<String, Object> result = new HashMap<>();
        result.put("moviesUpdated", moviesUpdated);
        result.put("episodesUpdated", episodesUpdated);
        result.put("message", "Backfill completed");

        return ResponseEntity.ok(result);
    }

    @Data
    public static class ImportRequest {
        private long tmdbId;
        private String type; // "movie" | "tv"
    }

    @Data
    public static class TrailerRequest {
        private String src;
    }
}

