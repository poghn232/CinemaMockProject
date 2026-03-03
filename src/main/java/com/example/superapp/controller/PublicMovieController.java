package com.example.superapp.controller;

import com.example.superapp.dto.MoviePageResponse;
import com.example.superapp.service.PublicMovieService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/movies")
@RequiredArgsConstructor
public class PublicMovieController {

    private final PublicMovieService publicMovieService;

    @GetMapping
    public MoviePageResponse list(
            @RequestParam(name = "type", defaultValue = "all") String type,
            @RequestParam(name = "page", defaultValue = "1") int page
    ) {
        return publicMovieService.listForHomepage(type, page);
    }
}

