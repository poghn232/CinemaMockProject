package com.example.superapp.service;

import com.example.superapp.dto.MovieDetailDto;
import com.example.superapp.dto.MovieItemDto;
import com.example.superapp.dto.MoviePageResponse;
import com.example.superapp.entity.*;
import com.example.superapp.repository.MovieRepository;
import com.example.superapp.repository.SubscriptionRepository;
import com.example.superapp.repository.TvSeriesRepository;
import com.example.superapp.repository.UserRepository;
import com.example.superapp.repository.VideoAssetRepository;
import com.example.superapp.repository.ReviewRepository;
import com.example.superapp.utils.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PublicMovieService {

    private final MovieRepository movieRepository;
    private final TvSeriesRepository tvSeriesRepository;
    private final TmdbService tmdbService;
    private final JwtUtils jwtUtils;
    private final VideoAssetService videoAssetService;
    private final R2StorageService r2StorageService;
    private final VideoAssetRepository videoAssetRepository;
    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final ReviewRepository reviewRepository;

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
        boolean premiumUser = isPremiumUser(request);

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

            return mapMovieDetail(m, premiumUser);
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

            return mapTvDetail(tv, premiumUser);
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

    private boolean isPremiumUser(HttpServletRequest request) {
        if (request == null) {
            return false;
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return false;
        }

        try {
            String token = authHeader.substring(7);
            String username = jwtUtils.extractUsername(token);

            return userRepository.findByUsername(username)
                    .map(user -> subscriptionRepository.existsByUser_UserIdAndStatusAndEndDateAfter(
                            user.getUserId(),
                            SubscriptionStatus.ACTIVE,
                            LocalDateTime.now()
                    ))
                    .orElse(false);
        } catch (Exception e) {
            return false;
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

    private MovieDetailDto mapMovieDetail(Movie m, boolean premiumUser) {
        MovieDetailDto dto = new MovieDetailDto();
        dto.setId(m.getId());
        dto.setType("movie");
        dto.setTitle(m.getTitle());
        dto.setOverview(m.getOverview());
        dto.setRating(m.getVoteAverage());
        dto.setVoteCount(m.getVoteCount());
        dto.setRuntime(m.getRuntime());
        dto.setPremiumUser(premiumUser);

        Double avg = reviewRepository.getAverageRatingForMovie(m.getId());
        Long count = reviewRepository.getRatingCountForMovie(m.getId());
        dto.setUserRating(avg != null ? avg : 0.0);
        dto.setUserRatingCount(count != null ? count : 0L);

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

        List<String> variants =
                r2StorageService.findVariantsWithoutDb("movie", m.getId());

        java.util.Optional<String> playbackOpt =
                r2StorageService.findPlaybackUrlWithoutDb("movie", m.getId());

        if (playbackOpt.isPresent()) {
            dto.setSrcFilm(playbackOpt.get());
        }

        dto.setVariants(variants);

        if (premiumUser) {
            dto.setAllowedVariants(variants);
        } else {
            dto.setAllowedVariants(
                    variants.contains("v0") ? List.of("v0") : Collections.emptyList()
            );

            if (dto.getSrcFilm() != null && !dto.getSrcFilm().isBlank() && variants.contains("v0")) {
                dto.setSrcFilm(buildVariantPlaylistUrl(dto.getSrcFilm(), "v0"));
            }
        }

        // Attach subtitle public URL if a default subtitle exists in R2 (prefer .vtt over .srt)
        try {
            String subtitlePrefix = "subtitles/movie/" + m.getId() + "/";
            String vttKey = subtitlePrefix + "default.vtt";
            String srtKey = subtitlePrefix + "default.srt";

            if (r2StorageService.objectExists(vttKey)) {
                dto.setSubtitleUrl("/api/public/movies/subtitle/" + m.getId() + "/default.vtt");
            } else if (r2StorageService.objectExists(srtKey)) {
                dto.setSubtitleUrl("/api/public/movies/subtitle/" + m.getId() + "/default.srt");
            } else {
                // fallback: check listing for any file under the prefix and use the first
                java.util.List<String> keys = r2StorageService.listObjectsByPrefix(subtitlePrefix);
                if (keys != null && !keys.isEmpty()) {
                    String first = keys.get(0);
                    String filename = first.substring(subtitlePrefix.length());
                    dto.setSubtitleUrl("/api/public/movies/subtitle/" + m.getId() + "/" + filename);
                }
            }
        } catch (Exception ignored) {
        }

        try {
            List<com.example.superapp.dto.CastMemberDto> cast = new ArrayList<>();
            if (m.getCredits() != null) {
                m.getCredits().stream()
                        .sorted(java.util.Comparator.comparing(mc -> mc.getCreditOrder() == null ? 0 : mc.getCreditOrder()))
                        .forEach(mc -> {
                            com.example.superapp.entity.Person p = mc.getPerson();
                            if (p == null) {
                                return;
                            }
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

        try {
            if (m.getCredits() != null) {
                m.getCredits().stream()
                        .filter(mc -> mc != null && mc.getJob() != null && "Director".equalsIgnoreCase(mc.getJob()))
                        .findFirst()
                        .ifPresent(mc -> {
                            if (mc.getPerson() != null) {
                                dto.setDirector(mc.getPerson().getName());
                            }
                        });
            }
        } catch (Exception ignored) {
        }

        try {
            if (m.getStudios() != null && !m.getStudios().isEmpty()) {
                m.getStudios().stream()
                        .filter(s -> s != null && s.getOriginCountry() != null && !s.getOriginCountry().isBlank())
                        .findFirst()
                        .ifPresent(s -> dto.setCountry(s.getOriginCountry()));
            }
        } catch (Exception ignored) {
        }

        try {
            if (m.getStudios() != null && !m.getStudios().isEmpty()) {
                m.getStudios().stream()
                        .filter(s -> s != null && s.getName() != null && !s.getName().isBlank())
                        .findFirst()
                        .ifPresent(s -> dto.setStudio(s.getName()));
            }
        } catch (Exception ignored) {
        }

        if ((dto.getDirector() == null || dto.getDirector().isBlank()) || (dto.getCountry() == null || dto.getCountry().isBlank())) {
            try {
                java.util.Map<String, Object> tm = tmdbService.getMovieDetails(m.getId());
                if (tm != null && !tm.isEmpty()) {
                    if ((dto.getDirector() == null || dto.getDirector().isBlank()) && tm.containsKey("credits")) {
                        Object creditsObj = tm.get("credits");
                        if (creditsObj instanceof java.util.Map<?, ?> creditsMap) {
                            Object crewObj = creditsMap.get("crew");
                            if (crewObj instanceof java.util.List<?> crewList) {
                                for (Object cobj : crewList) {
                                    if (!(cobj instanceof java.util.Map)) {
                                        continue;
                                    }
                                    java.util.Map<String, Object> crew = (java.util.Map<String, Object>) cobj;
                                    String job = TmdbService.stringVal(crew.get("job"));
                                    if (job != null && job.equalsIgnoreCase("Director")) {
                                        String name = TmdbService.stringVal(crew.get("name"));
                                        if (name != null) {
                                            dto.setDirector(name);
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if ((dto.getCountry() == null || dto.getCountry().isBlank()) && tm.containsKey("production_countries")) {
                        Object pcObj = tm.get("production_countries");
                        if (pcObj instanceof java.util.List<?> pcList && !pcList.isEmpty()) {
                            Object first = pcList.get(0);
                            if (first instanceof java.util.Map<?, ?> pm) {
                                String countryName = TmdbService.stringVal(pm.get("name"));
                                if (countryName == null) {
                                    countryName = TmdbService.stringVal(pm.get("iso_3166_1"));
                                }
                                if (countryName != null) {
                                    dto.setCountry(countryName);
                                }
                            }
                        }
                    }

                    if ((dto.getStudio() == null || dto.getStudio().isBlank()) && tm.containsKey("production_companies")) {
                        Object pcObj = tm.get("production_companies");
                        if (pcObj instanceof java.util.List<?> pcList && !pcList.isEmpty()) {
                            Object first = pcList.get(0);
                            if (first instanceof java.util.Map<?, ?> pm) {
                                String studioName = TmdbService.stringVal(pm.get("name"));
                                if (studioName != null) {
                                    dto.setStudio(studioName);
                                }
                            }
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }

        return dto;
    }

    private MovieDetailDto mapTvDetail(TvSeries tv, boolean premiumUser) {
        MovieDetailDto dto = new MovieDetailDto();
        dto.setId(tv.getId());
        dto.setType("tv");
        dto.setTitle(tv.getName());
        dto.setOverview(tv.getOverview());
        dto.setRating(tv.getVoteAverage());
        dto.setVoteCount(tv.getVoteCount());
        dto.setPremiumUser(premiumUser);

        Double avg = reviewRepository.getAverageRatingForTvSeries(tv.getId());
        Long count = reviewRepository.getRatingCountForTvSeries(tv.getId());
        dto.setUserRating(avg != null ? avg : 0.0);
        dto.setUserRatingCount(count != null ? count : 0L);

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
        dto.setVariants(Collections.emptyList());
        dto.setAllowedVariants(Collections.emptyList());

        try {
            List<com.example.superapp.dto.CastMemberDto> cast = new ArrayList<>();
            if (tv.getCredits() != null && !tv.getCredits().isEmpty()) {
                tv.getCredits().stream()
                        .sorted(java.util.Comparator.comparing(tc -> tc.getCreditOrder() == null ? 0 : tc.getCreditOrder()))
                        .forEach(tc -> {
                            com.example.superapp.entity.Person p = tc.getPerson();
                            if (p == null) {
                                return;
                            }
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
                                    if (!(item instanceof java.util.Map)) {
                                        continue;
                                    }
                                    java.util.Map<String, Object> cm = (java.util.Map<String, Object>) item;
                                    Object pidObj = cm.get("id");
                                    Long pid = null;
                                    if (pidObj instanceof Number n) {
                                        pid = n.longValue();
                                    }
                                    String pname = TmdbService.stringVal(cm.get("name"));
                                    String character = TmdbService.stringVal(cm.get("character"));
                                    String profilePath = TmdbService.stringVal(cm.get("profile_path"));
                                    if (pname == null) {
                                        continue;
                                    }
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

        try {
            if (tv.getCredits() != null && !tv.getCredits().isEmpty()) {
                tv.getCredits().stream()
                        .filter(tc -> tc != null && tc.getJob() != null && "Director".equalsIgnoreCase(tc.getJob()))
                        .findFirst()
                        .ifPresent(tc -> {
                            if (tc.getPerson() != null) {
                                dto.setDirector(tc.getPerson().getName());
                            }
                        });
            }
        } catch (Exception ignored) {
        }

        try {
            if (tv.getStudios() != null && !tv.getStudios().isEmpty()) {
                tv.getStudios().stream()
                        .filter(s -> s != null && s.getOriginCountry() != null && !s.getOriginCountry().isBlank())
                        .findFirst()
                        .ifPresent(s -> dto.setCountry(s.getOriginCountry()));
            }
        } catch (Exception ignored) {
        }

        try {
            if (tv.getStudios() != null && !tv.getStudios().isEmpty()) {
                tv.getStudios().stream()
                        .filter(s -> s != null && s.getName() != null && !s.getName().isBlank())
                        .findFirst()
                        .ifPresent(s -> dto.setStudio(s.getName()));
            }
        } catch (Exception ignored) {
        }

        if ((dto.getDirector() == null || dto.getDirector().isBlank()) || (dto.getCountry() == null || dto.getCountry().isBlank())) {
            try {
                java.util.Map<String, Object> tm = tmdbService.getTvDetails(tv.getId());
                if (tm != null && !tm.isEmpty()) {
                    if ((dto.getDirector() == null || dto.getDirector().isBlank()) && tm.containsKey("credits")) {
                        Object creditsObj = tm.get("credits");
                        if (creditsObj instanceof java.util.Map<?, ?> creditsMap) {
                            Object crewObj = creditsMap.get("crew");
                            if (crewObj instanceof java.util.List<?> crewList) {
                                for (Object cobj : crewList) {
                                    if (!(cobj instanceof java.util.Map)) {
                                        continue;
                                    }
                                    java.util.Map<String, Object> crew = (java.util.Map<String, Object>) cobj;
                                    String job = TmdbService.stringVal(crew.get("job"));
                                    if (job != null && job.equalsIgnoreCase("Director")) {
                                        String name = TmdbService.stringVal(crew.get("name"));
                                        if (name != null) {
                                            dto.setDirector(name);
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if ((dto.getCountry() == null || dto.getCountry().isBlank()) && tm.containsKey("production_countries")) {
                        Object pcObj = tm.get("production_countries");
                        if (pcObj instanceof java.util.List<?> pcList && !pcList.isEmpty()) {
                            Object first = pcList.get(0);
                            if (first instanceof java.util.Map<?, ?> pm) {
                                String countryName = TmdbService.stringVal(pm.get("name"));
                                if (countryName == null) {
                                    countryName = TmdbService.stringVal(pm.get("iso_3166_1"));
                                }
                                if (countryName != null) {
                                    dto.setCountry(countryName);
                                }
                            }
                        }
                    }
                    if ((dto.getStudio() == null || dto.getStudio().isBlank()) && tm.containsKey("production_companies")) {
                        Object pcObj = tm.get("production_companies");
                        if (pcObj instanceof java.util.List<?> pcList && !pcList.isEmpty()) {
                            Object first = pcList.get(0);
                            if (first instanceof java.util.Map<?, ?> pm) {
                                String studioName = TmdbService.stringVal(pm.get("name"));
                                if (studioName != null) {
                                    dto.setStudio(studioName);
                                }
                            }
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }

        return dto;
    }

    private String buildVariantPlaylistUrl(String masterUrl, String variantName) {
        if (masterUrl == null || masterUrl.isBlank() || variantName == null || variantName.isBlank()) {
            return masterUrl;
        }

        String marker = "/master.m3u8";
        int idx = masterUrl.lastIndexOf(marker);
        if (idx < 0) {
            return masterUrl;
        }

        String base = masterUrl.substring(0, idx);
        return base + "/" + variantName + "/playlist.m3u8";
    }

    @Transactional(readOnly = true)
    public java.util.List<com.example.superapp.dto.GenreWithItems> listGenresWithItems(HttpServletRequest request) {
        String userRegion = extractRegionFromRequest(request);

        List<com.example.superapp.dto.GenreWithItems> result = new ArrayList<>();
        java.util.Map<Long, com.example.superapp.dto.GenreWithItems> map = new java.util.HashMap<>();

        java.util.function.BiConsumer<Long, String> ensureGenre = (id, name) -> {
            if (id == null || name == null) {
                return;
            }
            if (!map.containsKey(id)) {
                map.put(id, new com.example.superapp.dto.GenreWithItems(id, name));
            }
        };

        for (Movie m : movieRepository.findByActiveTrueAndPublishedTrue()) {
            if (isMovieBlockedForRegion(m, userRegion)) {
                continue;
            }
            MovieItemDto item = mapMovie(m);
            if (m.getGenres() != null) {
                for (com.example.superapp.entity.Genre g : m.getGenres()) {
                    if (g == null) {
                        continue;
                    }
                    ensureGenre.accept(g.getId(), g.getName());
                    map.get(g.getId()).addItem(item);
                }
            }
        }

        for (TvSeries tv : tvSeriesRepository.findByActiveTrueAndPublishedTrue()) {
            if (isTvBlockedForRegion(tv, userRegion)) {
                continue;
            }
            MovieItemDto item = mapTv(tv);
            if (tv.getGenres() != null) {
                for (com.example.superapp.entity.Genre g : tv.getGenres()) {
                    if (g == null) {
                        continue;
                    }
                    ensureGenre.accept(g.getId(), g.getName());
                    map.get(g.getId()).addItem(item);
                }
            }
        }

        result.addAll(map.values());
        result.sort((a, b) -> Integer.compare(b.getItems().size(), a.getItems().size()));
        return result;
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public List<MovieItemDto> getTopRated(int limit, jakarta.servlet.http.HttpServletRequest request) {
        String userRegion = extractRegionFromRequest(request);
        int safeLimit = Math.max(1, limit);
        org.springframework.data.domain.PageRequest pr = org.springframework.data.domain.PageRequest.of(0, safeLimit);

        List<MovieItemDto> results = new ArrayList<>();

        // Get Top Movies
        List<Object[]> topMovies = reviewRepository.findTopMoviesByRating(pr);
        for (Object[] row : topMovies) {
            com.example.superapp.entity.Movie m = (com.example.superapp.entity.Movie) row[0];
            Double avg = (Double) row[1];
            if (m != null && !isMovieBlockedForRegion(m, userRegion)) {
                MovieItemDto dto = mapMovie(m);
                dto.setRating(avg != null ? avg : 0.0);
                results.add(dto);
            }
        }

        // Get Top TV Series
        List<Object[]> topTv = reviewRepository.findTopTvSeriesByRating(pr);
        for (Object[] row : topTv) {
            com.example.superapp.entity.TvSeries tv = (com.example.superapp.entity.TvSeries) row[0];
            Double avg = (Double) row[1];
            if (tv != null && !isTvBlockedForRegion(tv, userRegion)) {
                MovieItemDto dto = mapTv(tv);
                dto.setRating(avg != null ? avg : 0.0);
                results.add(dto);
            }
        }

        // Sort combined list by rating DESC
        results.sort((a, b) -> Double.compare(b.getRating(), a.getRating()));

        // Return the top N
        if (results.size() > safeLimit) {
            return results.subList(0, safeLimit);
        }
        return results;
    }
}