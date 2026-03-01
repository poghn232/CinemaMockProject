package com.example.superapp.controller;

import com.example.superapp.dto.MovieItemDto;
import com.example.superapp.service.TmdbService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/movies")
public class MovieController {

    private final TmdbService tmdbService;

    public MovieController(TmdbService tmdbService) {
        this.tmdbService = tmdbService;
    }

    @GetMapping("/trending")
    public List<MovieItemDto> trending(
            @RequestParam(name = "type", defaultValue = "all") String type
    ) {
        return tmdbService.getTrending(type);
    }
}
