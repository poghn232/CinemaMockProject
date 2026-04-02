package com.example.superapp.controller;

import com.example.superapp.dto.MoviePageResponse;
import com.example.superapp.dto.MovieDetailDto;
import com.example.superapp.service.PublicMovieService;
import com.example.superapp.service.R2StorageService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;
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
    private final R2StorageService r2StorageService;

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

    @GetMapping("/subtitle/{movieId}/{filename:.+}")
    @ResponseBody
    public ResponseEntity<byte[]> proxySubtitle(
            @PathVariable("movieId") Long movieId,
            @PathVariable("filename") String filename
    ) {
        String key = "subtitles/movie/" + movieId + "/" + filename;
        var opt = r2StorageService.getObjectBytes(key);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        var respBytes = opt.get();
        try {
            if (filename.toLowerCase().endsWith(".vtt")) {
                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType("text/vtt"))
                        .body(respBytes.asByteArray());
            }

            if (filename.toLowerCase().endsWith(".srt")) {
                // convert SRT -> WebVTT
                String srtText = new String(respBytes.asByteArray());
                StringBuilder vtt = new StringBuilder();
                vtt.append("WEBVTT\n\n");

                // Simple conversion: replace comma decimals with dot and keep blocks
                // Also remove Windows CR if present
                String normalized = srtText.replace("\r\n", "\n").replace("\r", "\n");
                String[] blocks = normalized.split("\n\n");
                for (String block : blocks) {
                    String trimmed = block.trim();
                    if (trimmed.isEmpty()) {
                        continue;
                    }
                    // if block starts with a number (index), drop it
                    String[] lines = trimmed.split("\n");
                    int idx = 0;
                    if (lines.length > 0 && lines[0].matches("^\\d+$")) {
                        idx = 1;
                    }
                    if (idx >= lines.length) {
                        continue;
                    }
                    // time line
                    String timeLine = lines[idx];
                    timeLine = timeLine.replace(',', '.');
                    vtt.append(timeLine).append('\n');
                    // rest lines
                    for (int i = idx + 1; i < lines.length; i++) {
                        vtt.append(lines[i]).append('\n');
                    }
                    vtt.append('\n');
                }

                byte[] out = vtt.toString().getBytes();
                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType("text/vtt"))
                        .body(out);
            }
        } catch (Exception e) {
            // fallback: return raw bytes
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(respBytes.asByteArray());
        }

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(respBytes.asByteArray());
    }
    @GetMapping("/top-rated")

    public java.util.List<com.example.superapp.dto.MovieItemDto> topRated(
            @RequestParam(name = "limit", defaultValue = "10") int limit,
            HttpServletRequest request
    ) {
        return publicMovieService.getTopRated(limit, request);
    }

    @GetMapping("/suggest")
    public java.util.List<com.example.superapp.dto.MovieItemDto> suggest(
            @RequestParam(name = "q") String q,
            @RequestParam(name = "limit", defaultValue = "10") int limit,
            HttpServletRequest request
    ) {
        return publicMovieService.suggest(q, limit, request);
    }
}
