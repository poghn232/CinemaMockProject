package com.example.superapp.service;

import com.example.superapp.dto.AdminMovieDto;
import com.example.superapp.entity.Movie;
import com.example.superapp.entity.TvSeries;
import com.example.superapp.repository.MovieRepository;
import com.example.superapp.repository.TvSeriesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminMovieService {

    private final MovieRepository movieRepository;
    private final TvSeriesRepository tvSeriesRepository;
    private final TmdbService tmdbService;

    @Transactional(readOnly = true)
    public List<AdminMovieDto> listPublished(String query) {
        String q = query == null ? "" : query.trim();

        List<Movie> movies = q.isBlank()
                ? movieRepository.findByActiveTrueAndPublishedTrue()
                : movieRepository.findByActiveTrueAndPublishedTrueAndTitleContainingIgnoreCase(q);

        List<TvSeries> tvs = q.isBlank()
                ? tvSeriesRepository.findByActiveTrueAndPublishedTrue()
                : tvSeriesRepository.findByActiveTrueAndPublishedTrueAndNameContainingIgnoreCase(q);

        List<AdminMovieDto> result = new ArrayList<>();

        for (Movie m : movies) {
            result.add(new AdminMovieDto(
                    m.getId(),
                    m.getTitle(),
                    "movie",
                    Boolean.TRUE.equals(m.getPublished()),
                    Boolean.TRUE.equals(m.getActive())
            ));
        }

        for (TvSeries tv : tvs) {
            result.add(new AdminMovieDto(
                    tv.getId(),
                    tv.getName(),
                    "tv",
                    Boolean.TRUE.equals(tv.getPublished()),
                    Boolean.TRUE.equals(tv.getActive())
            ));
        }

        result.sort(Comparator.comparing(AdminMovieDto::getTitle, String.CASE_INSENSITIVE_ORDER));
        return result;
    }

    @Transactional
    public AdminMovieDto importFromTmdb(long tmdbId, String type) {
        String t = (type == null ? "movie" : type.trim().toLowerCase());
        if (!t.equals("movie") && !t.equals("tv")) {
            throw new IllegalArgumentException("type must be 'movie' or 'tv'");
        }

        if (t.equals("movie")) {
            Movie existing = movieRepository.findById(tmdbId).orElse(null);
            if (existing != null) {
                existing.setActive(true);
                existing.setPublished(true);
                Movie saved = movieRepository.save(existing);
                return new AdminMovieDto(saved.getId(), saved.getTitle(), "movie",
                        Boolean.TRUE.equals(saved.getPublished()),
                        Boolean.TRUE.equals(saved.getActive()));
            }

            Map<String, Object> raw = tmdbService.getMovieDetails(tmdbId);
            Movie movie = mapMovieFromRaw(raw);
            movie.setId(tmdbId);
            movie.setActive(true);
            movie.setPublished(true);
            movie.setFeatured(false);

            Movie saved = movieRepository.save(movie);
            return new AdminMovieDto(saved.getId(), saved.getTitle(), "movie",
                    true, true);
        } else {
            TvSeries existing = tvSeriesRepository.findById(tmdbId).orElse(null);
            if (existing != null) {
                existing.setActive(true);
                existing.setPublished(true);
                TvSeries saved = tvSeriesRepository.save(existing);
                return new AdminMovieDto(saved.getId(), saved.getName(), "tv",
                        Boolean.TRUE.equals(saved.getPublished()),
                        Boolean.TRUE.equals(saved.getActive()));
            }

            Map<String, Object> raw = tmdbService.getTvDetails(tmdbId);
            TvSeries tv = mapTvFromRaw(raw);
            tv.setId(tmdbId);
            tv.setActive(true);
            tv.setPublished(true);
            tv.setFeatured(false);

            TvSeries saved = tvSeriesRepository.save(tv);
            return new AdminMovieDto(saved.getId(), saved.getName(), "tv",
                    true, true);
        }
    }

    @Transactional
    public void hide(long tmdbId, String type) {
        String t = (type == null ? "movie" : type.trim().toLowerCase());
        if (t.equals("movie")) {
            movieRepository.findById(tmdbId).ifPresent(m -> {
                m.setPublished(false);
                movieRepository.save(m);
            });
        } else if (t.equals("tv")) {
            tvSeriesRepository.findById(tmdbId).ifPresent(tv -> {
                tv.setPublished(false);
                tvSeriesRepository.save(tv);
            });
        } else {
            throw new IllegalArgumentException("type must be 'movie' or 'tv'");
        }
    }

    @SuppressWarnings("unchecked")
    private Movie mapMovieFromRaw(Map<String, Object> raw) {
        Movie m = new Movie();

        m.setTitle(TmdbService.stringVal(raw.get("title")));
        m.setOverview(TmdbService.stringVal(raw.get("overview")));
        m.setPosterPath(TmdbService.stringVal(raw.get("poster_path")));
        m.setBackdropPath(TmdbService.stringVal(raw.get("backdrop_path")));

        Object voteAverageObj = raw.get("vote_average");
        if (voteAverageObj instanceof Number n) {
            m.setVoteAverage(n.doubleValue());
        }
        Object voteCountObj = raw.get("vote_count");
        if (voteCountObj instanceof Number n) {
            m.setVoteCount(n.intValue());
        }

        String releaseDateStr = TmdbService.stringVal(raw.get("release_date"));
        if (releaseDateStr != null && !releaseDateStr.isBlank()) {
            try {
                m.setReleaseDate(LocalDate.parse(releaseDateStr));
            } catch (Exception ignored) {
            }
        }

        Object runtimeObj = raw.get("runtime");
        if (runtimeObj instanceof Number n) {
            m.setRuntime(n.intValue());
        }

        // Genres / studios / credits có thể map sau nếu cần chi tiết hơn

        return m;
    }

    private TvSeries mapTvFromRaw(Map<String, Object> raw) {
        TvSeries tv = new TvSeries();

        tv.setName(TmdbService.stringVal(raw.get("name")));
        tv.setOverview(TmdbService.stringVal(raw.get("overview")));
        tv.setPosterPath(TmdbService.stringVal(raw.get("poster_path")));
        tv.setBackdropPath(TmdbService.stringVal(raw.get("backdrop_path")));

        Object voteAverageObj = raw.get("vote_average");
        if (voteAverageObj instanceof Number n) {
            tv.setVoteAverage(n.doubleValue());
        }
        Object voteCountObj = raw.get("vote_count");
        if (voteCountObj instanceof Number n) {
            tv.setVoteCount(n.intValue());
        }

        String firstAirStr = TmdbService.stringVal(raw.get("first_air_date"));
        if (firstAirStr != null && !firstAirStr.isBlank()) {
            try {
                tv.setFirstAirDate(LocalDate.parse(firstAirStr));
            } catch (Exception ignored) {
            }
        }

        return tv;
    }
}

