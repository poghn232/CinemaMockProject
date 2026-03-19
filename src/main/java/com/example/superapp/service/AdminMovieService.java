package com.example.superapp.service;

import com.example.superapp.dto.AdminMovieDto;
import com.example.superapp.entity.*;
import com.example.superapp.repository.*;
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.units.qual.A;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.ui.ModelMap;
import org.springframework.web.server.ResponseStatusException;

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
    private final StudioRepository studioRepository;
    private final TvCreditRepository tvCreditRepository;
    private final TmdbService tmdbService;
    private final MovieRegionBlockRepository movieRegionBlockRepository;
    private final TvRegionBlockRepository tvRegionBlockRepository;
    private final AdminLogsRepository adminLogsRepository;
    private final AdminLogsService adminLogsService;
    private final NotificationService notificationService;


    @Transactional(readOnly = true)
    public List<AdminMovieDto> listPublished(String query) {
        String q = query == null ? "" : query.trim();
        System.out.println("[AdminMovieService] listPublished query='" + q + "'");

        List<Movie> movies = q.isBlank() ? movieRepository.findByActiveTrueAndPublishedTrue() : movieRepository.findByActiveTrueAndPublishedTrueAndTitleContainingIgnoreCase(q);

        List<TvSeries> tvs = q.isBlank() ? tvSeriesRepository.findByActiveTrueAndPublishedTrue() : tvSeriesRepository.findByActiveTrueAndPublishedTrueAndNameContainingIgnoreCase(q);

        List<AdminMovieDto> result = new ArrayList<>();

        for (Movie m : movies) {
            result.add(new AdminMovieDto(m.getId(), m.getTitle(), "movie", Boolean.TRUE.equals(m.getPublished()), Boolean.TRUE.equals(m.getActive()), m.getSrc()));
        }

        for (TvSeries tv : tvs) {
            result.add(new AdminMovieDto(tv.getId(), tv.getName(), "tv", Boolean.TRUE.equals(tv.getPublished()), Boolean.TRUE.equals(tv.getActive()), tv.getSrc()));
        }

        result.sort(Comparator.comparing(AdminMovieDto::getTitle, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
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
                } catch (Exception ignored) {
                }

                // replace genres
                try {
                    if (existing.getGenres() != null) existing.getGenres().clear();
                    if (fresh.getGenres() != null) existing.getGenres().addAll(fresh.getGenres());
                } catch (Exception ignored) {
                }

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
                            MovieCredit saved = movieCreditRepository.save(mc);
                            existing.getCredits().add(mc);
                        }
                    }
                } catch (Exception ignored) {
                }

                Movie saved = movieRepository.save(existing);
                adminLogsRepository.save(new AdminLogs(saved + " is added to database"));
                return new AdminMovieDto(saved.getId(), saved.getTitle(), "movie", Boolean.TRUE.equals(saved.getPublished()), Boolean.TRUE.equals(saved.getActive()), saved.getSrc());
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
            } catch (Exception ignored) {
            }

            Movie saved = movieRepository.save(movie);
            adminLogsRepository.save(new AdminLogs(saved + " is added to database"));

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
            } catch (Exception ignored) {
            }
            notificationService.notifyWishlistUsers(saved.getId(), "movie", saved.getTitle(), saved.getPosterPath(), "NEW_MOVIE");
            return new AdminMovieDto(saved.getId(), saved.getTitle(), "movie", true, true, saved.getSrc());
        } else {
            TvSeries existing = tvSeriesRepository.findById(tmdbId).orElse(null);
            if (existing != null) {
                // refresh simple metadata
                Map<String, Object> rawExisting = tmdbService.getTvDetails(tmdbId);
                TvSeries fresh = mapTvFromRaw(rawExisting);
                try {
                    existing.setName(fresh.getName());
                    existing.setOverview(fresh.getOverview());
                    existing.setPosterPath(fresh.getPosterPath());
                    existing.setBackdropPath(fresh.getBackdropPath());
                    existing.setVoteAverage(fresh.getVoteAverage());
                    existing.setVoteCount(fresh.getVoteCount());
                    existing.setFirstAirDate(fresh.getFirstAirDate());
                } catch (Exception ignored) {
                }

                // replace genres
                try {
                    if (existing.getGenres() != null) existing.getGenres().clear();
                    if (fresh.getGenres() != null) existing.getGenres().addAll(fresh.getGenres());
                } catch (Exception ignored) {
                }

                // replace studios
                try {
                    if (existing.getStudios() != null) existing.getStudios().clear();
                    if (fresh.getStudios() != null) existing.getStudios().addAll(fresh.getStudios());
                } catch (Exception ignored) {
                }

                existing.setActive(true);
                existing.setPublished(true);

                // replace credits: delete old then persist new credits linked to this tv
                try {
                    if (existing.getCredits() != null && !existing.getCredits().isEmpty()) {
                        tvCreditRepository.deleteAll(existing.getCredits());
                        existing.getCredits().clear();
                    }

                    if (fresh.getCredits() != null) {
                        for (com.example.superapp.entity.TvCredit tc : fresh.getCredits()) {
                            if (tc.getId() == null) tc.setId(new com.example.superapp.entity.TvCreditId());
                            tc.getId().setTvId(existing.getId());
                            tc.setTvSeries(existing);
                            if (tc.getPerson() != null && tc.getPerson().getId() != null) {
                                com.example.superapp.entity.Person p = personRepository.findById(tc.getPerson().getId()).orElse(tc.getPerson());
                                tc.setPerson(p);
                            }
                            TvCredit saved = tvCreditRepository.save(tc);
                            adminLogsRepository.save(new AdminLogs(saved + " is added to database"));

                            existing.getCredits().add(tc);
                        }
                    }
                } catch (Exception ignored) {
                }

                TvSeries saved = tvSeriesRepository.save(existing);
                adminLogsRepository.save(new AdminLogs(saved + " is added to database"));

                return new AdminMovieDto(saved.getId(), saved.getName(), "tv", Boolean.TRUE.equals(saved.getPublished()), Boolean.TRUE.equals(saved.getActive()), saved.getSrc());
            }

            Map<String, Object> raw = tmdbService.getTvDetails(tmdbId);
            TvSeries tv = mapTvFromRaw(raw);
            tv.setId(tmdbId);
            tv.setActive(true);
            tv.setPublished(true);
            tv.setFeatured(false);

            // detach credits before saving TV so JPA won't try to persist them with null tv refs
            java.util.Set<com.example.superapp.entity.TvCredit> credits = new java.util.HashSet<>();
            try {
                if (tv.getCredits() != null) {
                    credits.addAll(tv.getCredits());
                    tv.getCredits().clear();
                }
            } catch (Exception ignored) {
            }

            TvSeries saved = tvSeriesRepository.save(tv);
            adminLogsRepository.save(new AdminLogs(saved + " is added to database"));

            // persist credits after tv has an id and set proper tv reference + composite id
            try {
                for (com.example.superapp.entity.TvCredit tc : credits) {
                    if (tc.getId() == null) tc.setId(new com.example.superapp.entity.TvCreditId());
                    tc.getId().setTvId(saved.getId());
                    tc.setTvSeries(saved);
                    tvCreditRepository.save(tc);
                    saved.getCredits().add(tc);
                }
                tvSeriesRepository.save(saved);
                adminLogsRepository.save(new AdminLogs(saved + " is added to database"));
            } catch (Exception ignored) {
            }
            // ✅ Notify users có TV này trong wishlist
            notificationService.notifyWishlistUsers(
                    saved.getId(), "tv", saved.getName(), saved.getPosterPath(), "NEW_MOVIE"
            );
            return new AdminMovieDto(saved.getId(), saved.getName(), "tv", true, true, saved.getSrc());
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
            adminLogsRepository.save(new AdminLogs(tv + " is added to database"));
        }

        // fetch episode details via TMDB API: /tv/{tv_id}/season/{season_number}/episode/{episode_number}
        Map<String, Object> epRaw = tmdbService.getTvEpisodeDetails(tvId, seasonNumber, episodeNumber);
        // If TMDB didn't return details (404), fail the import so front-end doesn't mark as added.
        if (epRaw == null || epRaw.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "TMDB episode not found");
        }

        // upsert season
        Object seasonIdObj = epRaw.get("_season_id"); // TMDB doesn't provide a global season id in this payload; fall back to composite id
        Long seasonId = null;
        try {
            // derive a stable id: tvId * 1000 + seasonNumber (simple deterministic scheme)
            seasonId = tvId * 1000 + seasonNumber;
        } catch (Exception ignored) {
        }

        com.example.superapp.entity.Season season = null;
        if (seasonId != null) season = seasonRepository.findById(seasonId).orElse(null);
        if (season == null) {
            season = new com.example.superapp.entity.Season();
            season.setId(seasonId);
            season.setSeasonNumber(seasonNumber);
            season.setTvSeries(existing);
            seasonRepository.save(season);
            adminLogsRepository.save(new AdminLogs(season + " - " + season.getTvSeries() + " is added to database"));
        }

        // create / upsert episode
        Long epId = tvId * 100000L + seasonNumber * 1000L + episodeNumber; // deterministic composite id
        com.example.superapp.entity.Episode episode = episodeRepository.findById(epId).orElse(null);
        if (episode == null) episode = new com.example.superapp.entity.Episode();
        episode.setId(epId);
        episode.setName(TmdbService.stringVal(epRaw.get("name")));
        episode.setOverview(TmdbService.stringVal(epRaw.get("overview")));
        // episode_number must be present and numeric - fail import otherwise so UI doesn't show a false 'Added'
        Integer episodeNum = null;
        Object en = epRaw.get("episode_number");
        if (en instanceof Number n) {
            episodeNum = n.intValue();
        } else if (en instanceof String s) {
            try {
                episodeNum = Integer.parseInt(s.trim());
            } catch (Exception ignored) {
            }
        }
        if (episodeNum == null) {
            // TMDB returned no usable episode number - fail the import
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "TMDB episode missing episode_number");
        }
        episode.setEpisodeNumber(episodeNum);
        String air = TmdbService.stringVal(epRaw.get("air_date"));
        try {
            if (air != null && !air.isBlank()) episode.setAirDate(java.time.LocalDate.parse(air));
        } catch (Exception ignored) {
        }
        Object va = epRaw.get("vote_average");
        if (va instanceof Number nv) episode.setVoteAverage(nv.doubleValue());
        episode.setSeason(season);
        episode.setPublished(true); // Set published by default when importing
        episodeRepository.save(episode);
        adminLogsRepository.save(new AdminLogs(episode + " - " + episode.getSeason() + " - " + episode.getSeason().getTvSeries() + " is added to database"));
        // ✅ Notify users có TV series này trong wishlist
        notificationService.notifyWishlistUsers(
                existing.getId(), "tv", existing.getName(), existing.getPosterPath(),
                "NEW_SOURCE", epId, episode.getName()
        );
        return new AdminMovieDto(existing.getId(), existing.getName(), "tv", Boolean.TRUE.equals(existing.getPublished()), Boolean.TRUE.equals(existing.getActive()), existing.getSrc());
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
                adminLogsRepository.save(new AdminLogs(m + " is now hidden"));
                notificationService.notifyWishlistUsers(
                        m.getId(), "movie", m.getTitle(), m.getPosterPath(), "UNPUBLISHED"
                );
            });
        } else if (t.equals("tv")) {
            tvSeriesRepository.findById(tmdbId).ifPresent(tv -> {
                tv.setPublished(false);
                tvSeriesRepository.save(tv);
                adminLogsRepository.save(new AdminLogs(tv + " is now hidden"));
                notificationService.notifyWishlistUsers(
                        tv.getId(), "tv", tv.getName(), tv.getPosterPath(), "UNPUBLISHED"
                );
            });
        } else {
            throw new IllegalArgumentException("type must be 'movie' or 'tv'");
        }
    }

    @Transactional
    public void publish(long tmdbId, String type) {
        String t = (type == null ? "movie" : type.trim().toLowerCase());
        if (t.equals("movie")) {
            movieRepository.findById(tmdbId).ifPresent(m -> {
                m.setPublished(true);
                movieRepository.save(m);
                adminLogsRepository.save(new AdminLogs(m + " is now published"));
                notificationService.notifyWishlistUsers(
                        m.getId(), "movie", m.getTitle(), m.getPosterPath(), "PUBLISHED"
                );
            });
        } else if (t.equals("tv")) {
            tvSeriesRepository.findById(tmdbId).ifPresent(tv -> {
                tv.setPublished(true);
                tvSeriesRepository.save(tv);
                adminLogsRepository.save(new AdminLogs(tv + " is now published"));
                notificationService.notifyWishlistUsers(
                        tv.getId(), "tv", tv.getName(), tv.getPosterPath(), "PUBLISHED"
                );
            });
        } else {
            throw new IllegalArgumentException("type must be 'movie' or 'tv'");
        }
    }

    @Transactional
    public void toggleEpisodePublished(long tvId, int seasonNumber, int episodeNumber) {
        Long seasonId = tvId * 1000 + seasonNumber;
        Long epId = tvId * 100000L + seasonNumber * 1000L + episodeNumber;
        com.example.superapp.entity.Episode episode = episodeRepository.findById(epId).orElse(null);
        if (episode != null) {
            episode.setPublished(Boolean.TRUE.equals(episode.getPublished()) ? false : true);
            String status = episode.getPublished() ? "public" : "private";
            adminLogsRepository.save(new AdminLogs(episode.toString() + " is now " + status));
            episodeRepository.save(episode);
            // ✅ chỉ notify khi UNPUBLISH (published = false)
            if (!episode.getPublished()) {
                TvSeries tv = episode.getSeason().getTvSeries();
                notificationService.notifyWishlistUsers(
                        tv.getId(), "tv", tv.getName(), tv.getPosterPath(),
                        "UNPUBLISHED", epId, episode.getName()
                );
            }

        }
    }

    @Transactional
    public void setEpisodeTrailer(long tvId, int seasonNumber, int episodeNumber, String src) {
        Long epId = tvId * 100000L + seasonNumber * 1000L + episodeNumber;
        com.example.superapp.entity.Episode episode = episodeRepository.findById(epId).orElse(null);
        if (episode != null) {
            episode.setSrc(src);
            episodeRepository.save(episode);
            adminLogsRepository.save(new AdminLogs(episode.toString() + " updated trailer"));
            TvSeries tv = episode.getSeason().getTvSeries();
            notificationService.notifyWishlistUsers(
                    tv.getId(), "tv", tv.getName(), tv.getPosterPath(),
                    "NEW_TRAILER", epId, episode.getName()
            );
        }
    }

    @Transactional
    public void setMovieTrailer(long movieId, String src) {
        Movie movie = movieRepository.findById(movieId).orElse(null);
        if (movie != null) {
            movie.setSrc(src);
            movieRepository.save(movie);
            adminLogsRepository.save(new AdminLogs(movie.toString() + " updated trailer"));
            notificationService.notifyWishlistUsers(
                    movie.getId(), "movie", movie.getTitle(), movie.getPosterPath(), "NEW_TRAILER"
            );
        }
    }


    @Transactional(readOnly = true)
    public java.util.List<java.util.Map<String, Object>> listRegionBlocks(String type, long id) {
        String t = (type == null ? "movie" : type.trim().toLowerCase());
        java.util.List<java.util.Map<String, Object>> out = new java.util.ArrayList<>();
        if (t.equals("movie")) {
            java.util.List<com.example.superapp.entity.MovieRegionBlock> blocks = movieRegionBlockRepository.findByMovie_Id(id);
            for (com.example.superapp.entity.MovieRegionBlock b : blocks) {
                java.util.Map<String, Object> m = new java.util.HashMap<>();
                m.put("region", b.getRegionCode());
                m.put("blocked", true);
                out.add(m);
            }
        } else if (t.equals("tv")) {
            java.util.List<com.example.superapp.entity.TvRegionBlock> blocks = tvRegionBlockRepository.findByTvSeries_Id(id);
            for (com.example.superapp.entity.TvRegionBlock b : blocks) {
                java.util.Map<String, Object> m = new java.util.HashMap<>();
                m.put("region", b.getRegionCode());
                m.put("blocked", true);
                out.add(m);
            }
        } else {
            throw new IllegalArgumentException("type must be 'movie' or 'tv'");
        }
        return out;
    }

    @Transactional
    public void toggleRegionBlock(String type, long id, String regionCode) {
        if (regionCode == null || regionCode.isBlank()) return;
        String region = regionCode.trim().toUpperCase();
        String t = (type == null ? "movie" : type.trim().toLowerCase());

        if (t.equals("movie")) {
            com.example.superapp.entity.Movie m = movieRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Movie not found"));
            java.util.Optional<com.example.superapp.entity.MovieRegionBlock> existing = movieRegionBlockRepository.findByMovie_IdAndRegionCode(id, region);
            if (existing.isPresent()) {
                movieRegionBlockRepository.deleteByMovie_IdAndRegionCode(id, region);
                adminLogsRepository.save(new AdminLogs(m + " is now allowed in " + region));
            } else {
                com.example.superapp.entity.MovieRegionBlock b = new com.example.superapp.entity.MovieRegionBlock();
                b.setRegionCode(region);
                b.setMovie(m);
                movieRegionBlockRepository.save(b);
                adminLogsRepository.save(new AdminLogs(m + " is now blocked in " + region));
            }
        } else if (t.equals("tv")) {
            com.example.superapp.entity.TvSeries tv = tvSeriesRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("TV not found"));
            java.util.Optional<com.example.superapp.entity.TvRegionBlock> existing = tvRegionBlockRepository.findByTvSeries_IdAndRegionCode(id, region);
            if (existing.isPresent()) {
                tvRegionBlockRepository.deleteById(existing.get().getId());
                adminLogsRepository.save(new AdminLogs(tv + " is now allowed in " + region));
            } else {
                com.example.superapp.entity.TvRegionBlock b = new com.example.superapp.entity.TvRegionBlock();
                b.setRegionCode(region);
                b.setTvSeries(tv);
                tvRegionBlockRepository.save(b);
                adminLogsRepository.save(new AdminLogs(tv + " is now blocked in " + region));

            }
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
        } catch (Exception ignored) {
        }

        // Map production companies / studios (upsert by TMDB id)
        try {
            Object studiosObj = raw.get("production_companies");
            if (studiosObj instanceof java.util.List<?> studioList) {
                for (Object sObj : studioList) {
                    if (!(sObj instanceof Map)) continue;
                    Map<String, Object> sm = (Map<String, Object>) sObj;
                    Object idObj = sm.get("id");
                    Long sid = null;
                    if (idObj instanceof Number n) sid = n.longValue();
                    String sname = TmdbService.stringVal(sm.get("name"));
                    String logoPath = TmdbService.stringVal(sm.get("logo_path"));
                    String originCountry = TmdbService.stringVal(sm.get("origin_country"));
                    if (sid != null && sname != null && !sname.isBlank()) {
                        com.example.superapp.entity.Studio studio = studioRepository.findById(sid).orElse(null);
                        if (studio == null) {
                            studio = com.example.superapp.entity.Studio.builder().id(sid).name(sname).logoPath(logoPath).originCountry(originCountry).build();
                            studioRepository.save(studio);
                        } else {
                            boolean changed = false;
                            if (!sname.equals(studio.getName())) {
                                studio.setName(sname);
                                changed = true;
                            }
                            if (logoPath != null && !logoPath.equals(studio.getLogoPath())) {
                                studio.setLogoPath(logoPath);
                                changed = true;
                            }
                            if (originCountry != null && !originCountry.equals(studio.getOriginCountry())) {
                                studio.setOriginCountry(originCountry);
                                changed = true;
                            }
                            if (changed) studioRepository.save(studio);
                        }
                        m.getStudios().add(studio);
                    }
                }
            }
        } catch (Exception ignored) {
        }

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
                            if (!pname.equals(person.getName())) {
                                person.setName(pname);
                                changed = true;
                            }
                            if (profilePath != null && !profilePath.equals(person.getProfilePath())) {
                                person.setProfilePath(profilePath);
                                changed = true;
                            }
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
        } catch (Exception ignored) {
        }

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
                        tv.getGenres().add(genre);
                    }
                }
            }
        } catch (Exception ignored) {
        }

        // Map production companies / studios (upsert by TMDB id)
        try {
            Object studiosObj = raw.get("production_companies");
            if (studiosObj == null) studiosObj = raw.get("networks"); // TMDB uses networks for TV
            if (studiosObj instanceof java.util.List<?> studioList) {
                for (Object sObj : studioList) {
                    if (!(sObj instanceof Map)) continue;
                    Map<String, Object> sm = (Map<String, Object>) sObj;
                    Object idObj = sm.get("id");
                    Long sid = null;
                    if (idObj instanceof Number n) sid = n.longValue();
                    String sname = TmdbService.stringVal(sm.get("name"));
                    String logoPath = TmdbService.stringVal(sm.get("logo_path"));
                    String originCountry = TmdbService.stringVal(sm.get("origin_country"));
                    if (sid != null && sname != null && !sname.isBlank()) {
                        com.example.superapp.entity.Studio studio = studioRepository.findById(sid).orElse(null);
                        if (studio == null) {
                            studio = com.example.superapp.entity.Studio.builder().id(sid).name(sname).logoPath(logoPath).originCountry(originCountry).build();
                            studioRepository.save(studio);
                        } else {
                            boolean changed = false;
                            if (!sname.equals(studio.getName())) {
                                studio.setName(sname);
                                changed = true;
                            }
                            if (logoPath != null && !logoPath.equals(studio.getLogoPath())) {
                                studio.setLogoPath(logoPath);
                                changed = true;
                            }
                            if (originCountry != null && !originCountry.equals(studio.getOriginCountry())) {
                                studio.setOriginCountry(originCountry);
                                changed = true;
                            }
                            if (changed) studioRepository.save(studio);
                        }
                        tv.getStudios().add(studio);
                    }
                }
            }
        } catch (Exception ignored) {
        }

        // Map credits (cast) -> create Person entries and TvCredit records
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
                            if (!pname.equals(person.getName())) {
                                person.setName(pname);
                                changed = true;
                            }
                            if (profilePath != null && !profilePath.equals(person.getProfilePath())) {
                                person.setProfilePath(profilePath);
                                changed = true;
                            }
                            if (changed) personRepository.save(person);
                        }

                        // create TvCredit link (TvSeries not persisted yet, so set id later)
                        com.example.superapp.entity.TvCredit tc = new com.example.superapp.entity.TvCredit();
                        com.example.superapp.entity.TvCreditId tcid = new com.example.superapp.entity.TvCreditId();
                        tcid.setTvId(null); // will set after tv has an id
                        tcid.setPersonId(pid);
                        tc.setId(tcid);
                        tc.setPerson(person);
                        tc.setCharacter(character);
                        tc.setCreditOrder(order++);
                        tv.getCredits().add(tc);
                    }
                }
            }
        } catch (Exception ignored) {
        }

        return tv;
    }
}

