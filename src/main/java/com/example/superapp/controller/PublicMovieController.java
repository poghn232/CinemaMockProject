package com.example.superapp.controller;

import com.example.superapp.dto.MoviePageResponse;
import com.example.superapp.dto.MovieDetailDto;
import com.example.superapp.service.PublicMovieService;
import jakarta.servlet.http.HttpServletRequest;
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
            @RequestParam(name = "page", defaultValue = "1") int page,
            HttpServletRequest request
    ) {
        return publicMovieService.listForHomepage(type, page, request);
    }

    @GetMapping("/search")
    public MoviePageResponse search(
            @RequestParam(name = "q") String q,
            @RequestParam(name = "type", defaultValue = "all") String type,
            @RequestParam(name = "page", defaultValue = "1") int page,
            HttpServletRequest request
    ) {
        return publicMovieService.search(q, type, page, request);
    }

    @GetMapping("/detail")
    public MovieDetailDto detail(
            @RequestParam(name = "type") String type,
            @RequestParam(name = "id") long id,
            HttpServletRequest request
    ) {
        return publicMovieService.getDetail(type, id, request);
    }

    @GetMapping("/genres")
    public java.util.List<com.example.superapp.dto.GenreWithItems> genres(HttpServletRequest request) {
        return publicMovieService.listGenresWithItems(request);
    }
}