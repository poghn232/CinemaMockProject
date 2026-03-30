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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

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
    private final com.example.superapp.service.R2StorageService r2StorageService;

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
            // ensure subtitles folder exists for this movie in R2
            try {
                r2StorageService.createFolder("subtitles/movie/" + request.getTmdbId());
            } catch (Exception ignored) {}
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

    @PostMapping("/{type}/{id}/subtitle")
    public ResponseEntity<Map<String, Object>> uploadSubtitle(@PathVariable("type") String type,
                                                              @PathVariable("id") long id,
                                                              @RequestParam("file") MultipartFile file) {
        String t = (type == null ? "" : type.trim().toLowerCase());
        if (!"movie".equals(t)) {
            throw new IllegalArgumentException("Subtitle upload currently only supported for movies");
        }

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Empty subtitle file");
        }

        try {
            // create folder
            String folder = "subtitles/movie/" + id;
            r2StorageService.createFolder(folder);

            // write to temp file and upload
            Path tmp = Files.createTempFile("subtitle-", ".vtt");
            try (var in = file.getInputStream()) {
                Files.copy(in, tmp, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }

            // preserve extension from original filename if possible
            String orig = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
            String ext = "vtt";
            int dot = orig.lastIndexOf('.');
            if (dot >= 0 && dot + 1 < orig.length()) {
                ext = orig.substring(dot + 1).toLowerCase();
            }

            String objectKey = folder + "/default." + ext;
            String contentType = file.getContentType();
            if (contentType == null) {
                if ("srt".equals(ext)) contentType = "text/plain";
                else if ("vtt".equals(ext)) contentType = "text/vtt";
                else contentType = "application/octet-stream";
            }

            r2StorageService.uploadFile(tmp, objectKey, contentType);

            Files.deleteIfExists(tmp);

            return ResponseEntity.ok(Map.of("uploaded", true, "objectKey", objectKey));
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload subtitle: " + e.getMessage(), e);
        }
    }

    // Debug: list subtitle objects in R2 for a movie
    @GetMapping("/movie/{id}/subtitles")
    public ResponseEntity<java.util.List<java.util.Map<String,String>>> listMovieSubtitles(@PathVariable("id") long id) {
        String prefix = "subtitles/movie/" + id + "/";
        java.util.List<String> keys = r2StorageService.listObjectsByPrefix(prefix);

        java.util.List<java.util.Map<String,String>> out = keys.stream().map(k -> java.util.Map.of(
                "key", k,
                "url", r2StorageService.buildPublicUrl(k)
        )).toList();

        return ResponseEntity.ok(out);
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

