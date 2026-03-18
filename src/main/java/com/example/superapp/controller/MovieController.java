package com.example.superapp.controller;

import com.example.superapp.dto.MoviePageResponse;
import com.example.superapp.service.TmdbService;
import com.example.superapp.entity.TvSeries;
import com.example.superapp.entity.Season;
import com.example.superapp.entity.Episode;
import com.example.superapp.repository.TvSeriesRepository;
import com.example.superapp.repository.SeasonRepository;
import com.example.superapp.repository.EpisodeRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
@RequestMapping("/api/movies")
public class MovieController {

    private final TmdbService tmdbService;
    private final TvSeriesRepository tvSeriesRepository;
    private final SeasonRepository seasonRepository;
    private final EpisodeRepository episodeRepository;

    public MovieController(TmdbService tmdbService, TvSeriesRepository tvSeriesRepository, SeasonRepository seasonRepository, EpisodeRepository episodeRepository) {
        this.tmdbService = tmdbService;
        this.tvSeriesRepository = tvSeriesRepository;
        this.seasonRepository = seasonRepository;
        this.episodeRepository = episodeRepository;
    }

    @GetMapping("/trending")
    public MoviePageResponse trending(
            @RequestParam(name = "type", defaultValue = "all") String type,
            @RequestParam(name = "page", defaultValue = "1") int page
    ) {
        return tmdbService.getTrending(type, page);
    }

    @GetMapping("/details")
    public Map<String, Object> details(@RequestParam("id") long tmdbId, @RequestParam(name = "type", defaultValue = "movie") String type) {
        String t = type == null ? "movie" : type.trim().toLowerCase();
        if (t.equals("tv")) {
            return tmdbService.getTvDetails(tmdbId);
        }
        return tmdbService.getMovieDetails(tmdbId);
    }

    @GetMapping("/search")
    public MoviePageResponse search(@RequestParam("q") String q, @RequestParam(name = "type", defaultValue = "all") String type, @RequestParam(name = "page", defaultValue = "1") int page) {
        return tmdbService.search(q, type, page);
    }

        @GetMapping("/tv/{tvId}/season/{season}/episode/{episode}")
        public Map<String, Object> episodeDetails(@PathVariable("tvId") long tvId,
                                                   @PathVariable("season") int season,
                                                   @PathVariable("episode") int episode) {
            return tmdbService.getTvEpisodeDetails(tvId, season, episode);
        }

        @GetMapping("/tv/{tvId}/seasons")
        public java.util.List<java.util.Map<String, Object>> getSeasons(@PathVariable("tvId") long tvId) {
            System.out.println("[MovieController] getSeasons tvId=" + tvId);
            TvSeries tv = tvSeriesRepository.findById(tvId).orElse(null);
            if (tv == null) {
                System.out.println("[MovieController] TV not found: " + tvId);
                return java.util.Collections.emptyList();
            }
            java.util.List<com.example.superapp.entity.Season> seasons = seasonRepository.findByTvSeriesId(tvId);
            System.out.println("[MovieController] Found " + seasons.size() + " seasons");
            return seasons.stream().map(s -> {
                java.util.Map<String, Object> m = new java.util.HashMap<>();
                m.put("seasonNumber", s.getSeasonNumber());
                java.util.List<java.util.Map<String, Object>> eps = episodeRepository.findBySeasonId(s.getId()).stream()
                        .filter(e -> Boolean.TRUE.equals(e.getPublished())) // Only published episodes
                        .map(e -> {
                    java.util.Map<String, Object> em = new java.util.HashMap<>();
                    em.put("id", e.getId());
                    em.put("episodeNumber", e.getEpisodeNumber());
                    em.put("name", e.getName());
                    em.put("src", e.getSrc());
                    em.put("published", e.getPublished());
                    return em;
                }).collect(java.util.stream.Collectors.toList());
                m.put("episodes", eps);
                System.out.println("[MovieController] Season " + s.getSeasonNumber() + " has " + eps.size() + " published episodes");
                return m;
            }).collect(java.util.stream.Collectors.toList());
        }
}
