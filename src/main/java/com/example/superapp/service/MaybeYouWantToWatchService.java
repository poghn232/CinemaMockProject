package com.example.superapp.service;

import com.example.superapp.dto.MovieItemDto;
import com.example.superapp.entity.Genre;
import com.example.superapp.entity.Movie;
import com.example.superapp.entity.TvSeries;
import com.example.superapp.entity.User;
import com.example.superapp.entity.WatchHistory;
import com.example.superapp.repository.MovieRepository;
import com.example.superapp.repository.TvSeriesRepository;
import com.example.superapp.repository.UserRepository;
import com.example.superapp.repository.WatchHistoryRepository;
import com.example.superapp.utils.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MaybeYouWantToWatchService {

    private final WatchHistoryRepository watchHistoryRepository;
    private final UserRepository userRepository;
    private final MovieRepository movieRepository;
    private final TvSeriesRepository tvSeriesRepository;
    private final JwtUtils jwtUtils;

    @Value("${tmdb.image-base-url}")
    private String imageBaseUrl;

    @Transactional(readOnly = true)
    public List<MovieItemDto> recommend(Authentication auth, HttpServletRequest request, int limit) {
        if (auth == null) {
            return List.of();
        }

        int safeLimit = Math.max(1, Math.min(limit, 24));
        User user = userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<WatchHistory> history = watchHistoryRepository.findByUser_UserIdOrderByWatchedAtDesc(user.getUserId());
        if (history == null || history.isEmpty()) {
            return List.of();
        }

        Set<Long> watchedMovieIds = new HashSet<>();
        Set<Long> watchedTvSeriesIds = new HashSet<>();
        Map<String, Integer> genreFreq = new HashMap<>();

        for (WatchHistory h : history) {
            if (h == null) continue;

            if (h.getMovie() != null) {
                Movie m = h.getMovie();
                watchedMovieIds.add(m.getId());
                addGenresToFreq(genreFreq, extractGenreNames(m.getGenres()));
            } else if (h.getEpisode() != null && h.getEpisode().getSeason() != null && h.getEpisode().getSeason().getTvSeries() != null) {
                TvSeries tv = h.getEpisode().getSeason().getTvSeries();
                watchedTvSeriesIds.add(tv.getId());
                addGenresToFreq(genreFreq, extractGenreNames(tv.getGenres()));
            }
        }

        if (genreFreq.isEmpty()) {
            return List.of();
        }

        // Keep it small: pick top-N genre names by frequency from watch history.
        Set<String> desiredGenres = genreFreq.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .limit(5)
                .map(Map.Entry::getKey)
                .filter(s -> s != null && !s.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (desiredGenres.isEmpty()) {
            return List.of();
        }

        String userRegion = extractRegionFromRequest(request);

        // Priority: rating desc, then year desc.
        Map<Long, MovieItemDto> candidateMap = new HashMap<>();

        for (Movie m : movieRepository.findByActiveTrueAndPublishedTrue()) {
            if (m == null) continue;
            if (watchedMovieIds.contains(m.getId())) continue;
            if (isMovieBlockedForRegion(m, userRegion)) continue;

            Set<String> mGenres = extractGenreNames(m.getGenres());
            if (Collections.disjoint(mGenres, desiredGenres)) continue;

            candidateMap.putIfAbsent(m.getId(), mapMovie(m));
        }

        for (TvSeries tv : tvSeriesRepository.findByActiveTrueAndPublishedTrue()) {
            if (tv == null) continue;
            if (watchedTvSeriesIds.contains(tv.getId())) continue;
            if (isTvBlockedForRegion(tv, userRegion)) continue;

            Set<String> tvGenres = extractGenreNames(tv.getGenres());
            if (Collections.disjoint(tvGenres, desiredGenres)) continue;

            candidateMap.putIfAbsent(tv.getId(), mapTv(tv));
        }

        List<MovieItemDto> candidates = new ArrayList<>(candidateMap.values());
        candidates.sort((a, b) -> {
            double ra = a.getRating() == null ? 0.0 : a.getRating();
            double rb = b.getRating() == null ? 0.0 : b.getRating();
            int cmp = Double.compare(rb, ra);
            if (cmp != 0) return cmp;
            Integer ya = a.getYear();
            Integer yb = b.getYear();
            return (yb == null ? 0 : yb) - (ya == null ? 0 : ya);
        });

        if (candidates.isEmpty()) return List.of();
        return candidates.stream().limit(safeLimit).collect(Collectors.toList());
    }

    private void addGenresToFreq(Map<String, Integer> freq, Set<String> genres) {
        if (genres == null || genres.isEmpty()) return;
        for (String g : genres) {
            if (g == null || g.isBlank()) continue;
            freq.merge(g, 1, Integer::sum);
        }
    }

    private Set<String> extractGenreNames(Set<Genre> genres) {
        if (genres == null || genres.isEmpty()) return Set.of();
        return genres.stream()
                .filter(Objects::nonNull)
                .map(Genre::getName)
                .map(s -> s == null ? null : s.trim())
                .filter(s -> s != null && !s.isBlank())
                // Normalize for case-insensitive matching
                .map(s -> s.toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());
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

    private String extractRegionFromRequest(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        try {
            String token = authHeader.substring(7);
            String region = jwtUtils.extractRegion(token);
            if (region == null || region.isBlank()) {
                return null;
            }
            return region.trim().toUpperCase();
        } catch (Exception e) {
            return null;
        }
    }

    private boolean isMovieBlockedForRegion(Movie movie, String userRegion) {
        if (userRegion == null || userRegion.isBlank()) return false;
        if (movie.getRegionBlocks() == null || movie.getRegionBlocks().isEmpty()) return false;
        return movie.getRegionBlocks().stream()
                .map(b -> b.getRegionCode())
                .filter(code -> code != null && !code.isBlank())
                .anyMatch(code -> code.trim().equalsIgnoreCase(userRegion));
    }

    private boolean isTvBlockedForRegion(TvSeries tvSeries, String userRegion) {
        if (userRegion == null || userRegion.isBlank()) return false;
        if (tvSeries.getRegionBlocks() == null || tvSeries.getRegionBlocks().isEmpty()) return false;
        return tvSeries.getRegionBlocks().stream()
                .map(b -> b.getRegionCode())
                .filter(code -> code != null && !code.isBlank())
                .anyMatch(code -> code.trim().equalsIgnoreCase(userRegion));
    }
}

