package com.example.superapp.controller;

import com.example.superapp.dto.MoviePageResponse;
import com.example.superapp.service.TmdbService;
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

    public MovieController(TmdbService tmdbService) {
        this.tmdbService = tmdbService;
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

        @GetMapping("/tv/{tvId}/season/{season}/episode/{episode}")
        public Map<String, Object> episodeDetails(@PathVariable("tvId") long tvId,
                                                   @PathVariable("season") int season,
                                                   @PathVariable("episode") int episode) {
            return tmdbService.getTvEpisodeDetails(tvId, season, episode);
        }
}
