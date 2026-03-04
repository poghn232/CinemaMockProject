package com.example.superapp.controller;

import com.example.superapp.dto.AdminMovieDto;
import com.example.superapp.service.AdminMovieService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/movies")
@RequiredArgsConstructor
public class AdminMovieController {

    private final AdminMovieService adminMovieService;

    @GetMapping
    public List<AdminMovieDto> list(@RequestParam(name = "q", required = false) String query) {
        return adminMovieService.listPublished(query);
    }

    @PostMapping("/import")
    public AdminMovieDto importMovie(@RequestBody ImportRequest request) {
        return adminMovieService.importFromTmdb(request.getTmdbId(), request.getType());
    }

    @PutMapping("/{type}/{id}/hide")
    public void hide(@PathVariable("type") String type, @PathVariable("id") long id) {
        adminMovieService.hide(id, type);
    }

    @Data
    public static class ImportRequest {
        private long tmdbId;
        private String type; // "movie" | "tv"
    }
}

