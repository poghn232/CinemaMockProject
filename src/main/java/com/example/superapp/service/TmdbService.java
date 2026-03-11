package com.example.superapp.service;

import com.example.superapp.dto.MovieItemDto;
import com.example.superapp.dto.MoviePageResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class TmdbService {

    private final String apiKey;
    private final String baseUrl;
    private final String imageBaseUrl;
    private final RestTemplate restTemplate = new RestTemplate();

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

    public static String stringVal(Object value) {
        return value == null ? null : String.valueOf(value).trim();
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getMovieDetails(long tmdbId) {
    // include credits (cast) in the response to support admin detail view
    String url = baseUrl + "/movie/" + tmdbId + "?language=en-US&api_key=" + apiKey + "&append_to_response=credits";
        Map<String, Object> response = restTemplate.getForObject(url, Map.class);
        return response == null ? Map.of() : response;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getTvDetails(long tmdbId) {
    // include credits (cast) for TV as well
    String url = baseUrl + "/tv/" + tmdbId + "?language=en-US&api_key=" + apiKey + "&append_to_response=credits";
        Map<String, Object> response = restTemplate.getForObject(url, Map.class);
        return response == null ? Map.of() : response;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getTvEpisodeDetails(long tvId, int seasonNumber, int episodeNumber) {
        String url = baseUrl + "/tv/" + tvId + "/season/" + seasonNumber + "/episode/" + episodeNumber + "?language=en-US&api_key=" + apiKey + "&append_to_response=credits";
        Map<String, Object> response = restTemplate.getForObject(url, Map.class);
        return response == null ? Map.of() : response;
    }
}
