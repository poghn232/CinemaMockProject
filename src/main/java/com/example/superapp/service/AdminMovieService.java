package com.example.superapp.service;

import com.example.superapp.dto.AdminMovieDto;
import com.example.superapp.entity.Movie;
import com.example.superapp.entity.TvSeries;
import com.example.superapp.repository.MovieRepository;
import com.example.superapp.repository.TvSeriesRepository;
import com.example.superapp.repository.SeasonRepository;
import com.example.superapp.repository.EpisodeRepository;
import com.example.superapp.repository.GenreRepository;
import com.example.superapp.repository.PersonRepository;
import com.example.superapp.repository.MovieCreditRepository;
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
    private final SeasonRepository seasonRepository;
    private final EpisodeRepository episodeRepository;
    private final GenreRepository genreRepository;
    private final PersonRepository personRepository;
    private final MovieCreditRepository movieCreditRepository;
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
                // refresh metadata from TMDB and merge
                Map<String, Object> rawExisting = tmdbService.getMovieDetails(tmdbId);
                Movie fresh = mapMovieFromRaw(rawExisting);

                // copy simple fields
                try {
                    existing.setTitle(fresh.getTitle());
                    existing.setOverview(fresh.getOverview());
                    existing.setPosterPath(fresh.getPosterPath());
                    existing.setBackdropPath(fresh.getBackdropPath());
                    existing.setVoteAverage(fresh.getVoteAverage());
                    existing.setVoteCount(fresh.getVoteCount());
                    existing.setReleaseDate(fresh.getReleaseDate());
                    existing.setRuntime(fresh.getRuntime());
                } catch (Exception ignored) {}

                // replace genres
                try {
                    if (existing.getGenres() != null) existing.getGenres().clear();
                    if (fresh.getGenres() != null) existing.getGenres().addAll(fresh.getGenres());
                } catch (Exception ignored) {}

                existing.setActive(true);
                existing.setPublished(true);

                // replace credits: delete old then persist new credits linked to this movie
                try {
                    if (existing.getCredits() != null && !existing.getCredits().isEmpty()) {
                        movieCreditRepository.deleteAll(existing.getCredits());
                        existing.getCredits().clear();
                    }

                    if (fresh.getCredits() != null) {
                        for (com.example.superapp.entity.MovieCredit mc : fresh.getCredits()) {
                            if (mc.getId() == null) mc.setId(new com.example.superapp.entity.MovieCreditId());
                            mc.getId().setMovieId(existing.getId());
                            mc.setMovie(existing);
                            // ensure person is persisted (mapMovieFromRaw already upserted persons)
                            if (mc.getPerson() != null && mc.getPerson().getId() != null) {
                                com.example.superapp.entity.Person p = personRepository.findById(mc.getPerson().getId()).orElse(mc.getPerson());
                                mc.setPerson(p);
                            }
                            movieCreditRepository.save(mc);
                            existing.getCredits().add(mc);
                        }
                    }
                } catch (Exception ignored) {}

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

            // detach credits before saving movie so JPA won't try to persist them with null movie refs
            java.util.Set<com.example.superapp.entity.MovieCredit> credits = new java.util.HashSet<>();
            try {
                if (movie.getCredits() != null) {
                    credits.addAll(movie.getCredits());
                    movie.getCredits().clear();
                }
            } catch (Exception ignored) {}

            Movie saved = movieRepository.save(movie);

            // persist credits after movie has an id and set proper movie reference + composite id
            try {
                for (com.example.superapp.entity.MovieCredit mc : credits) {
                    if (mc.getId() == null) mc.setId(new com.example.superapp.entity.MovieCreditId());
                    mc.getId().setMovieId(saved.getId());
                    mc.setMovie(saved);
                    movieCreditRepository.save(mc);
                    // attach back to saved movie entity
                    saved.getCredits().add(mc);
                }
                // save movie again to update relationship if needed
                movieRepository.save(saved);
            } catch (Exception ignored) {}
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
    public AdminMovieDto importEpisodeFromTmdb(long tvId, int seasonNumber, int episodeNumber) {
        // fetch TV details to ensure tv exists
        Map<String, Object> tvRaw = tmdbService.getTvDetails(tvId);
        TvSeries existing = tvSeriesRepository.findById(tvId).orElse(null);
        if (existing == null) {
            TvSeries tv = mapTvFromRaw(tvRaw);
            tv.setId(tvId);
            tv.setActive(true);
            tv.setPublished(true);
            existing = tvSeriesRepository.save(tv);
        }

        // fetch episode details via TMDB API: /tv/{tv_id}/season/{season_number}/episode/{episode_number}
        Map<String, Object> epRaw = tmdbService.getTvEpisodeDetails(tvId, seasonNumber, episodeNumber);

        // upsert season
        Object seasonIdObj = epRaw.get("_season_id"); // TMDB doesn't provide a global season id in this payload; fall back to composite id
        Long seasonId = null;
        try {
            // derive a stable id: tvId * 1000 + seasonNumber (simple deterministic scheme)
            seasonId = tvId * 1000 + seasonNumber;
        } catch (Exception ignored) {}

        com.example.superapp.entity.Season season = null;
        if (seasonId != null) season = seasonRepository.findById(seasonId).orElse(null);
        if (season == null) {
            season = new com.example.superapp.entity.Season();
            season.setId(seasonId);
            season.setSeasonNumber(seasonNumber);
            season.setTvSeries(existing);
            seasonRepository.save(season);
        }

        // create / upsert episode
        Long epId = tvId * 100000L + seasonNumber * 1000L + episodeNumber; // deterministic composite id
        com.example.superapp.entity.Episode episode = episodeRepository.findById(epId).orElse(null);
        if (episode == null) episode = new com.example.superapp.entity.Episode();
        episode.setId(epId);
        episode.setName(TmdbService.stringVal(epRaw.get("name")));
        episode.setOverview(TmdbService.stringVal(epRaw.get("overview")));
        Object en = epRaw.get("episode_number"); if (en instanceof Number n) episode.setEpisodeNumber(n.intValue());
        String air = TmdbService.stringVal(epRaw.get("air_date"));
        try { if (air != null && !air.isBlank()) episode.setAirDate(java.time.LocalDate.parse(air)); } catch (Exception ignored) {}
        Object va = epRaw.get("vote_average"); if (va instanceof Number nv) episode.setVoteAverage(nv.doubleValue());
        episode.setSeason(season);
        episodeRepository.save(episode);

        return new AdminMovieDto(existing.getId(), existing.getName(), "tv", Boolean.TRUE.equals(existing.getPublished()), Boolean.TRUE.equals(existing.getActive()));
    }

    @Transactional(readOnly = true)
    public java.util.List<Integer> getExistingEpisodeNumbers(long tvId, int seasonNumber) {
        Long seasonId = tvId * 1000 + seasonNumber;
        java.util.List<com.example.superapp.entity.Episode> eps = episodeRepository.findBySeasonId(seasonId);
        java.util.List<Integer> numbers = new java.util.ArrayList<>();
        for (com.example.superapp.entity.Episode e : eps) {
            if (e.getEpisodeNumber() != null) numbers.add(e.getEpisodeNumber());
        }
        return numbers;
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

        // Map genres (upsert by TMDB id)
        try {
            Object genresObj = raw.get("genres");
            if (genresObj instanceof java.util.List<?> genreList) {
                for (Object gObj : genreList) {
                    if (!(gObj instanceof Map)) continue;
                    Map<String, Object> gm = (Map<String, Object>) gObj;
                    Object idObj = gm.get("id");
                    Long gid = null;
                    if (idObj instanceof Number n) gid = n.longValue();
                    String gname = TmdbService.stringVal(gm.get("name"));
                    if (gid != null && gname != null && !gname.isBlank()) {
                        com.example.superapp.entity.Genre genre = genreRepository.findById(gid).orElse(null);
                        if (genre == null) {
                            genre = new com.example.superapp.entity.Genre(gid, gname);
                            genreRepository.save(genre);
                        } else if (!gname.equals(genre.getName())) {
                            genre.setName(gname);
                            genreRepository.save(genre);
                        }
                        m.getGenres().add(genre);
                    }
                }
            }
        } catch (Exception ignored) {}

        // Map credits (cast) -> create Person entries and MovieCredit records
        try {
            Object creditsObj = raw.get("credits");
            if (creditsObj instanceof Map<?, ?> creditsMap) {
                Object castObj = creditsMap.get("cast");
                if (castObj instanceof java.util.List<?> castList) {
                    int order = 0;
                    for (Object cObj : castList) {
                        if (!(cObj instanceof Map)) continue;
                        Map<String, Object> cm = (Map<String, Object>) cObj;
                        Object pidObj = cm.get("id");
                        Long pid = null;
                        if (pidObj instanceof Number n) pid = n.longValue();
                        String pname = TmdbService.stringVal(cm.get("name"));
                        String profilePath = TmdbService.stringVal(cm.get("profile_path"));
                        String character = TmdbService.stringVal(cm.get("character"));
                        if (pid == null || pname == null) continue;

                        com.example.superapp.entity.Person person = personRepository.findById(pid).orElse(null);
                        if (person == null) {
                            person = new com.example.superapp.entity.Person();
                            person.setId(pid);
                            person.setName(pname);
                            person.setProfilePath(profilePath);
                            personRepository.save(person);
                        } else {
                            boolean changed = false;
                            if (!pname.equals(person.getName())) { person.setName(pname); changed = true; }
                            if (profilePath != null && !profilePath.equals(person.getProfilePath())) { person.setProfilePath(profilePath); changed = true; }
                            if (changed) personRepository.save(person);
                        }

                        // create MovieCredit link (Movie not persisted yet, so set id later by save cascade)
                        com.example.superapp.entity.MovieCredit mc = new com.example.superapp.entity.MovieCredit();
                        com.example.superapp.entity.MovieCreditId mcid = new com.example.superapp.entity.MovieCreditId();
                        mcid.setMovieId(null); // will set after movie has an id
                        mcid.setPersonId(pid);
                        mc.setId(mcid);
                        mc.setPerson(person);
                        mc.setCharacter(character);
                        mc.setCreditOrder(order++);
                        // store in movie's credits set; when movie saved, ensure movie field and id are populated
                        m.getCredits().add(mc);
                    }
                }
            }
        } catch (Exception ignored) {}

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

