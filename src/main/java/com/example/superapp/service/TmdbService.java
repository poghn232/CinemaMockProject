package com.example.superapp.service;

import com.example.superapp.dto.MovieItemDto;
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
    public List<MovieItemDto> getTrending(String type) {
        String mediaType = type == null || type.isBlank() ? "all" : type.trim().toLowerCase();
        if (!mediaType.equals("movie") && !mediaType.equals("tv")) {
            mediaType = "all";
        }

        String url = baseUrl + "/trending/" + mediaType + "/day?language=en-US&api_key=" + apiKey;

        Map<String, Object> response = restTemplate.getForObject(url, Map.class);
        if (response == null) {
            return List.of();
        }

        Object resultsObj = response.get("results");
        if (!(resultsObj instanceof List<?> rawList)) {
            return List.of();
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
        return items;
    }

    private static String stringVal(Object value) {
        return value == null ? null : String.valueOf(value).trim();
    }
}
