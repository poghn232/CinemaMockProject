package com.example.superapp.service;

import com.example.superapp.dto.MovieItemDto;
import com.example.superapp.dto.MoviePageResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple in-memory TTL cache for TMDB responses keyed by URL.
 */
class CacheEntry {
    final Map<String, Object> value;
    final long ts;

    CacheEntry(Map<String, Object> value, long ts) {
        this.value = value;
        this.ts = ts;
    }
}

@Service
public class TmdbService {

    private final String apiKey;
    private final String baseUrl;
    private final String imageBaseUrl;
    private final RestTemplate restTemplate = new RestTemplate();
    private static final Logger log = LoggerFactory.getLogger(TmdbService.class);
    private final ConcurrentHashMap<String, CacheEntry> cache = new ConcurrentHashMap<>();

    @Value("${tmdb.cache-ttl-seconds:1800}")
    private long cacheTtlSeconds;

    public TmdbService(
            @Value("${tmdb.api-key}") String apiKey,
            @Value("${tmdb.base-url}") String baseUrl,
            @Value("${tmdb.image-base-url}") String imageBaseUrl
    ) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.imageBaseUrl = imageBaseUrl;
    }

    /**
     * Gọi TMDB Trending API.
     * @param type "all" | "movie" | "tv"
     */
    @SuppressWarnings("unchecked")
    public MoviePageResponse getTrending(String type, int page) {
        String mediaType = type == null || type.isBlank() ? "all" : type.trim().toLowerCase();
        if (!mediaType.equals("movie") && !mediaType.equals("tv")) {
            mediaType = "all";
        }

        int safePage = page <= 0 ? 1 : page;

        String url =
                baseUrl
                        + "/trending/"
                        + mediaType
                        + "/day?language=en-US&page="
                        + safePage
                        + "&api_key="
                        + apiKey;

        Map<String, Object> response = restTemplate.getForObject(url, Map.class);
        if (response == null) {
            return new MoviePageResponse(List.of(), safePage, 1);
        }

        Object pageObj = response.get("page");
        Object totalPagesObj = response.get("total_pages");

        int currentPage = (pageObj instanceof Number n) ? n.intValue() : safePage;
        int totalPages =
                (totalPagesObj instanceof Number n) ? Math.max(1, n.intValue()) : 1;

        Object resultsObj = response.get("results");
        if (!(resultsObj instanceof List<?> rawList)) {
            return new MoviePageResponse(List.of(), currentPage, totalPages);
        }

        List<MovieItemDto> items = new ArrayList<>();
        for (Object element : rawList) {
            if (!(element instanceof Map)) {
                continue;
            }
            Map<String, Object> row = (Map<String, Object>) element;

            MovieItemDto dto = new MovieItemDto();

            Object idObj = row.get("id");
            if (idObj instanceof Number n) {
                dto.setId(n.longValue());
            }

            String mediaTypeValue = stringVal(row.get("media_type"));
            if (mediaTypeValue == null || mediaTypeValue.isBlank()) {
                mediaTypeValue = mediaType;
            }
            dto.setType(mediaTypeValue);

            String title = stringVal(row.get("title"));
            if (title == null || title.isBlank()) {
                title = stringVal(row.get("name"));
            }
            dto.setTitle(title);

            Object voteObj = row.get("vote_average");
            if (voteObj instanceof Number n) {
                dto.setRating(n.doubleValue());
            }

            String dateStr = stringVal(row.get("release_date"));
            if (dateStr == null || dateStr.isBlank()) {
                dateStr = stringVal(row.get("first_air_date"));
            }
            if (dateStr != null && dateStr.length() >= 4) {
                try {
                    dto.setYear(LocalDate.parse(dateStr.substring(0, 10)).getYear());
                } catch (Exception ignored) {
                    try {
                        dto.setYear(Integer.parseInt(dateStr.substring(0, 4)));
                    } catch (Exception ignored2) {
                        // skip
                    }
                }
            }

            String posterPath = stringVal(row.get("poster_path"));
            if (posterPath != null && !posterPath.isBlank()) {
                dto.setImageUrl(imageBaseUrl + posterPath);
            }

            if (dto.getTitle() != null) {
                items.add(dto);
            }
        }

        return new MoviePageResponse(items, currentPage, totalPages);
    }

    /**
     * Search TMDB for query across movie/tv or specific type.
     */
    @SuppressWarnings("unchecked")
    public MoviePageResponse search(String query, String type, int page) {
        if (query == null) query = "";
        String q = query.trim();
        if (q.isBlank()) return new MoviePageResponse(List.of(), 1, 1);

        String t = (type == null ? "all" : type.trim().toLowerCase());
        String endpoint;
        if (t.equals("movie")) endpoint = "/search/movie";
        else if (t.equals("tv")) endpoint = "/search/tv";
        else endpoint = "/search/multi";

        int safePage = page <= 0 ? 1 : page;
        String url = baseUrl + endpoint + "?language=en-US&page=" + safePage + "&query=" + URLEncoder.encode(q, StandardCharsets.UTF_8) + "&api_key=" + apiKey;

        Map<String, Object> response = safeGetMap(url);
        if (response == null) return new MoviePageResponse(List.of(), safePage, 1);

        Object pageObj = response.get("page");
        Object totalPagesObj = response.get("total_pages");

        int currentPage = (pageObj instanceof Number n) ? n.intValue() : safePage;
        int totalPages = (totalPagesObj instanceof Number n) ? Math.max(1, n.intValue()) : 1;

        Object resultsObj = response.get("results");
        if (!(resultsObj instanceof List<?> rawList)) {
            return new MoviePageResponse(List.of(), currentPage, totalPages);
        }

        List<MovieItemDto> items = new ArrayList<>();
        for (Object element : rawList) {
            if (!(element instanceof Map)) continue;
            Map<String, Object> row = (Map<String, Object>) element;
            MovieItemDto dto = new MovieItemDto();

            Object idObj = row.get("id");
            if (idObj instanceof Number n) dto.setId(n.longValue());

            String mediaTypeValue = stringVal(row.get("media_type"));
            if (mediaTypeValue == null || mediaTypeValue.isBlank()) {
                // fallback: infer from endpoint
                mediaTypeValue = t.equals("all") ? (row.containsKey("title") ? "movie" : "tv") : t;
            }
            dto.setType(mediaTypeValue);

            String title = stringVal(row.get("title"));
            if (title == null || title.isBlank()) title = stringVal(row.get("name"));
            dto.setTitle(title);

            Object voteObj = row.get("vote_average");
            if (voteObj instanceof Number n) dto.setRating(n.doubleValue());

            String dateStr = stringVal(row.get("release_date"));
            if (dateStr == null || dateStr.isBlank()) dateStr = stringVal(row.get("first_air_date"));
            if (dateStr != null && dateStr.length() >= 4) {
                try {
                    dto.setYear(LocalDate.parse(dateStr.substring(0, 10)).getYear());
                } catch (Exception ignored) {
                    try { dto.setYear(Integer.parseInt(dateStr.substring(0, 4))); } catch (Exception ignored2) {}
                }
            }

            String posterPath = stringVal(row.get("poster_path"));
            if (posterPath != null && !posterPath.isBlank()) dto.setImageUrl(imageBaseUrl + posterPath);

            if (dto.getTitle() != null) items.add(dto);
        }

        return new MoviePageResponse(items, currentPage, totalPages);
    }

    public static String stringVal(Object value) {
        return value == null ? null : String.valueOf(value).trim();
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getMovieDetails(long tmdbId) {
    // include credits (cast) in the response to support admin detail view
    String url = baseUrl + "/movie/" + tmdbId + "?language=en-US&api_key=" + apiKey + "&append_to_response=credits";
        Map<String, Object> response = safeGetMap(url);
    return response == null ? Map.of() : response;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getTvDetails(long tmdbId) {
    // include credits (cast) for TV as well
    String url = baseUrl + "/tv/" + tmdbId + "?language=en-US&api_key=" + apiKey + "&append_to_response=credits";
        Map<String, Object> response = safeGetMap(url);
    return response == null ? Map.of() : response;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getTvEpisodeDetails(long tvId, int seasonNumber, int episodeNumber) {
        String url = baseUrl + "/tv/" + tvId + "/season/" + seasonNumber + "/episode/" + episodeNumber + "?language=en-US&api_key=" + apiKey + "&append_to_response=credits";
        Map<String, Object> response = safeGetMap(url);
        return response == null ? Map.of() : response;
    }

    /**
     * Wrapper to call TMDB and return an empty map when resource not found (404).
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> safeGetMap(String url) {
        try {
            // simple cache by URL
            CacheEntry entry = cache.get(url);
            long now = System.currentTimeMillis();
            if (entry != null && (now - entry.ts) <= cacheTtlSeconds * 1000L) {
                return entry.value == null ? Map.of() : entry.value;
            }

            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            Map<String, Object> safe = response == null ? Map.of() : response;
            cache.put(url, new CacheEntry(safe, now));
            return safe;
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                log.warn("TMDB resource not found (404) for URL: {}", url);
                cache.put(url, new CacheEntry(Map.of(), System.currentTimeMillis()));
                return Map.of();
            }
            // rethrow other client errors
            throw e;
        }
    }
}
