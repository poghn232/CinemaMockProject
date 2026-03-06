package com.example.superapp.service;

import com.example.superapp.dto.MovieItemDto;
import com.example.superapp.dto.MoviePageResponse;
import com.example.superapp.entity.Movie;
import com.example.superapp.entity.TvSeries;
import com.example.superapp.repository.MovieRepository;
import com.example.superapp.repository.TvSeriesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PublicMovieService {

    private final MovieRepository movieRepository;
    private final TvSeriesRepository tvSeriesRepository;

    @Value("${tmdb.image-base-url}")
    private String imageBaseUrl;

    @Transactional(readOnly = true)
    public MoviePageResponse listForHomepage(String type, int page) {
        String t = type == null ? "all" : type.trim().toLowerCase();
        if (!t.equals("movie") && !t.equals("tv")) {
            t = "all";
        }

        int pageSize = 20;
        int safePage = Math.max(1, page);

        List<MovieItemDto> allItems = new ArrayList<>();

        if (t.equals("movie") || t.equals("all")) {
            for (Movie m : movieRepository.findByActiveTrueAndPublishedTrue()) {
                allItems.add(mapMovie(m));
            }
        }

        if (t.equals("tv") || t.equals("all")) {
            for (TvSeries tv : tvSeriesRepository.findByActiveTrueAndPublishedTrue()) {
                allItems.add(mapTv(tv));
            }
        }

        allItems.sort(Comparator.comparing(MovieItemDto::getTitle, String.CASE_INSENSITIVE_ORDER));

        int total = allItems.size();
        int totalPages = Math.max(1, (int) Math.ceil(total / (double) pageSize));

        int fromIndex = (safePage - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, total);
        if (fromIndex >= total) {
            fromIndex = 0;
            toIndex = Math.min(pageSize, total);
            safePage = 1;
        }

        List<MovieItemDto> pageItems = allItems.subList(fromIndex, toIndex);

        return new MoviePageResponse(pageItems, safePage, totalPages);
    }

    private MovieItemDto mapMovie(Movie m) {
        MovieItemDto dto = new MovieItemDto();
        dto.setId(m.getId());
        dto.setType("movie");
        dto.setTitle(m.getTitle());
        dto.setRating(m.getVoteAverage());

        LocalDate release = m.getReleaseDate();
        if (release != null) {
            dto.setYear(release.getYear());
        }

        if (m.getPosterPath() != null && !m.getPosterPath().isBlank()) {
            dto.setImageUrl(imageBaseUrl + m.getPosterPath());
        }

        return dto;
    }

    private MovieItemDto mapTv(TvSeries tv) {
        MovieItemDto dto = new MovieItemDto();
        dto.setId(tv.getId());
        dto.setType("tv");
        dto.setTitle(tv.getName());
        dto.setRating(tv.getVoteAverage());

        LocalDate firstAir = tv.getFirstAirDate();
        if (firstAir != null) {
            dto.setYear(firstAir.getYear());
        }

        if (tv.getPosterPath() != null && !tv.getPosterPath().isBlank()) {
            dto.setImageUrl(imageBaseUrl + tv.getPosterPath());
        }

        return dto;
    }
}

