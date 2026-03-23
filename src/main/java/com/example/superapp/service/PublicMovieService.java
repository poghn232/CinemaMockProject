package com.example.superapp.service;

import com.example.superapp.dto.MovieDetailDto;
import com.example.superapp.dto.MovieItemDto;
import com.example.superapp.dto.MoviePageResponse;
import com.example.superapp.entity.Movie;
import com.example.superapp.entity.MovieRegionBlock;
import com.example.superapp.entity.TvRegionBlock;
import com.example.superapp.entity.TvSeries;
import com.example.superapp.repository.MovieRepository;
import com.example.superapp.repository.TvSeriesRepository;
import com.example.superapp.utils.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PublicMovieService {

    private final MovieRepository movieRepository;
    private final TvSeriesRepository tvSeriesRepository;
    private final TmdbService tmdbService;
    private final JwtUtils jwtUtils;

    @Value("${tmdb.image-base-url}")
    private String imageBaseUrl;

    @Transactional(readOnly = true)
    public MoviePageResponse listForHomepage(String type, int page, HttpServletRequest request) {
        String t = type == null ? "all" : type.trim().toLowerCase();
        if (!t.equals("movie") && !t.equals("tv")) {
            t = "all";
        }

        String userRegion = extractRegionFromRequest(request);

        int pageSize = 20;
        int safePage = Math.max(1, page);

        List<MovieItemDto> allItems = new ArrayList<>();

        if (t.equals("movie") || t.equals("all")) {
            for (Movie m : movieRepository.findByActiveTrueAndPublishedTrue()) {
                if (!isMovieBlockedForRegion(m, userRegion)) {
                    allItems.add(mapMovie(m));
                }
            }
        }

        if (t.equals("tv") || t.equals("all")) {
            for (TvSeries tv : tvSeriesRepository.findByActiveTrueAndPublishedTrue()) {
                if (!isTvBlockedForRegion(tv, userRegion)) {
                    allItems.add(mapTv(tv));
                }
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

    @Transactional(readOnly = true)
    public MoviePageResponse search(String query, String type, int page, HttpServletRequest request) {
        String q = query == null ? "" : query.trim().toLowerCase();
        String t = type == null ? "all" : type.trim().toLowerCase();
        if (!t.equals("movie") && !t.equals("tv")) {
            t = "all";
        }

        String userRegion = extractRegionFromRequest(request);

        int pageSize = 20;
        int safePage = Math.max(1, page);

        List<MovieItemDto> allItems = new ArrayList<>();

        if (t.equals("movie") || t.equals("all")) {
            for (Movie m : movieRepository.findByActiveTrueAndPublishedTrue()) {
                if (!isMovieBlockedForRegion(m, userRegion)) {
                    MovieItemDto dto = mapMovie(m);
                    if (q.isEmpty() || (dto.getTitle() != null && dto.getTitle().toLowerCase().contains(q))) {
                        allItems.add(dto);
                    }
                }
            }
        }

        if (t.equals("tv") || t.equals("all")) {
            for (TvSeries tv : tvSeriesRepository.findByActiveTrueAndPublishedTrue()) {
                if (!isTvBlockedForRegion(tv, userRegion)) {
                    MovieItemDto dto = mapTv(tv);
                    if (q.isEmpty() || (dto.getTitle() != null && dto.getTitle().toLowerCase().contains(q))) {
                        allItems.add(dto);
                    }
                }
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

    @Transactional(readOnly = true)
    public MovieDetailDto getDetail(String type, long id, HttpServletRequest request) {
        String t = type == null ? "movie" : type.trim().toLowerCase();
        if (!t.equals("movie") && !t.equals("tv")) {
            throw new IllegalArgumentException("type must be 'movie' or 'tv'");
        }

        String userRegion = extractRegionFromRequest(request);

        if (t.equals("movie")) {
            Movie m = movieRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Movie not found"));

            if (!Boolean.TRUE.equals(m.getActive()) || !Boolean.TRUE.equals(m.getPublished())) {
                throw new IllegalArgumentException("Movie is not published");
            }

            if (isMovieBlockedForRegion(m, userRegion)) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Nội dung này không khả dụng tại khu vực của bạn"
                );
            }

            return mapMovieDetail(m);
        } else {
            TvSeries tv = tvSeriesRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("TV series not found"));

            if (!Boolean.TRUE.equals(tv.getActive()) || !Boolean.TRUE.equals(tv.getPublished())) {
                throw new IllegalArgumentException("TV series is not published");
            }

            if (isTvBlockedForRegion(tv, userRegion)) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Nội dung này không khả dụng tại khu vực của bạn"
                );
            }

            return mapTvDetail(tv);
        }
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
        if (userRegion == null || userRegion.isBlank()) {
            return false;
        }

        Set<MovieRegionBlock> blocks = movie.getRegionBlocks();
        if (blocks == null || blocks.isEmpty()) {
            return false;
        }

        return blocks.stream()
                .map(MovieRegionBlock::getRegionCode)
                .filter(code -> code != null && !code.isBlank())
                .anyMatch(code -> code.trim().equalsIgnoreCase(userRegion));
    }

    private boolean isTvBlockedForRegion(TvSeries tvSeries, String userRegion) {
        if (userRegion == null || userRegion.isBlank()) {
            return false;
        }

        Set<TvRegionBlock> blocks = tvSeries.getRegionBlocks();
        if (blocks == null || blocks.isEmpty()) {
            return false;
        }

        return blocks.stream()
                .map(TvRegionBlock::getRegionCode)
                .filter(code -> code != null && !code.isBlank())
                .anyMatch(code -> code.trim().equalsIgnoreCase(userRegion));
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

    private MovieDetailDto mapMovieDetail(Movie m) {
        MovieDetailDto dto = new MovieDetailDto();
        dto.setId(m.getId());
        dto.setType("movie");
        dto.setTitle(m.getTitle());
        dto.setOverview(m.getOverview());
        dto.setRating(m.getVoteAverage());
        dto.setVoteCount(m.getVoteCount());
        dto.setRuntime(m.getRuntime());

        LocalDate release = m.getReleaseDate();
        if (release != null) {
            dto.setYear(release.getYear());
        }

        if (m.getPosterPath() != null && !m.getPosterPath().isBlank()) {
            dto.setPosterUrl(imageBaseUrl + m.getPosterPath());
        }
        if (m.getBackdropPath() != null && !m.getBackdropPath().isBlank()) {
            dto.setBackdropUrl(imageBaseUrl + m.getBackdropPath());
        }

        dto.setSrc(m.getSrc());
        dto.setSrcFilm(m.getSrcFilm());

        try {
            List<com.example.superapp.dto.CastMemberDto> cast = new ArrayList<>();
            if (m.getCredits() != null) {
                m.getCredits().stream()
                        .sorted(java.util.Comparator.comparing(mc -> mc.getCreditOrder() == null ? 0 : mc.getCreditOrder()))
                        .forEach(mc -> {
                            com.example.superapp.entity.Person p = mc.getPerson();
                            if (p == null) return;
                            com.example.superapp.dto.CastMemberDto c = new com.example.superapp.dto.CastMemberDto();
                            c.setId(p.getId());
                            c.setName(p.getName());
                            c.setCharacter(mc.getCharacter());
                            if (p.getProfilePath() != null && !p.getProfilePath().isBlank()) {
                                c.setProfilePath(imageBaseUrl + p.getProfilePath());
                            } else {
                                c.setProfilePath(null);
                            }
                            cast.add(c);
                        });
            }
            dto.setCast(cast);
        } catch (Exception ignored) {
        }

        return dto;
    }

    private MovieDetailDto mapTvDetail(TvSeries tv) {
        MovieDetailDto dto = new MovieDetailDto();
        dto.setId(tv.getId());
        dto.setType("tv");
        dto.setTitle(tv.getName());
        dto.setOverview(tv.getOverview());
        dto.setRating(tv.getVoteAverage());
        dto.setVoteCount(tv.getVoteCount());

        LocalDate firstAir = tv.getFirstAirDate();
        if (firstAir != null) {
            dto.setYear(firstAir.getYear());
        }

        if (tv.getPosterPath() != null && !tv.getPosterPath().isBlank()) {
            dto.setPosterUrl(imageBaseUrl + tv.getPosterPath());
        }
        if (tv.getBackdropPath() != null && !tv.getBackdropPath().isBlank()) {
            dto.setBackdropUrl(imageBaseUrl + tv.getBackdropPath());
        }

        dto.setSrc(tv.getSrc());

        try {
            List<com.example.superapp.dto.CastMemberDto> cast = new ArrayList<>();
            if (tv.getCredits() != null && !tv.getCredits().isEmpty()) {
                tv.getCredits().stream()
                        .sorted(java.util.Comparator.comparing(tc -> tc.getCreditOrder() == null ? 0 : tc.getCreditOrder()))
                        .forEach(tc -> {
                            com.example.superapp.entity.Person p = tc.getPerson();
                            if (p == null) return;
                            com.example.superapp.dto.CastMemberDto c = new com.example.superapp.dto.CastMemberDto();
                            c.setId(p.getId());
                            c.setName(p.getName());
                            c.setCharacter(tc.getCharacter());
                            c.setProfilePath(p.getProfilePath());
                            cast.add(c);
                        });
            } else {
                try {
                    java.util.Map<String, Object> raw = tmdbService.getTvDetails(tv.getId());
                    if (raw != null) {
                        Object creditsObj = raw.get("credits");
                        if (creditsObj instanceof java.util.Map<?, ?> creditsMap) {
                            Object castObj = creditsMap.get("cast");
                            if (castObj instanceof java.util.List<?> castList) {
                                for (Object item : castList) {
                                    if (!(item instanceof java.util.Map)) continue;
                                    java.util.Map<String, Object> cm = (java.util.Map<String, Object>) item;
                                    Object pidObj = cm.get("id");
                                    Long pid = null;
                                    if (pidObj instanceof Number n) pid = n.longValue();
                                    String pname = TmdbService.stringVal(cm.get("name"));
                                    String character = TmdbService.stringVal(cm.get("character"));
                                    String profilePath = TmdbService.stringVal(cm.get("profile_path"));
                                    if (pname == null) continue;
                                    com.example.superapp.dto.CastMemberDto c = new com.example.superapp.dto.CastMemberDto();
                                    c.setId(pid);
                                    c.setName(pname);
                                    c.setCharacter(character);
                                    if (profilePath != null && !profilePath.isBlank()) {
                                        c.setProfilePath(imageBaseUrl + profilePath);
                                    }
                                    cast.add(c);
                                }
                            }
                        }
                    }
                } catch (Exception ignored2) {
                }
            }
            dto.setCast(cast);
        } catch (Exception ignored) {
        }

        return dto;
    }
}