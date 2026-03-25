package com.example.superapp.service;

import com.example.superapp.dto.AiChatRequest;
import com.example.superapp.dto.GroqResponse;
import com.example.superapp.entity.Genre;
import com.example.superapp.entity.Movie;
import com.example.superapp.entity.TvSeries;
import com.example.superapp.repository.MovieRepository;
import com.example.superapp.repository.TvSeriesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.HttpClientErrorException;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AiChatService {

    @Value("${groq.api-key-1:}")
    private String apiKey1;

    @Value("${groq.api-key-2:}")
    private String apiKey2;

    @Value("${groq.api-key-3:}")
    private String apiKey3;

    private static final String GROQ_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String GROQ_MODEL = "llama-3.3-70b-versatile";

    private final RestTemplate restTemplate = new RestTemplate();
    private final MovieRepository movieRepository;
    private final TvSeriesRepository tvSeriesRepository;

    // Thêm logger sau class declaration
    private static final Logger log = LoggerFactory.getLogger(AiChatService.class);

    // Thêm 2 method helper
    private List<String> getApiKeys() {
        return List.of(apiKey1, apiKey2, apiKey3)
                .stream()
                .filter(k -> k != null && !k.isBlank())
                .collect(Collectors.toList());
    }

    private String callGroq(List<Map<String, String>> messages, String apiKey) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", GROQ_MODEL);
        body.put("messages", messages);
        body.put("max_tokens", 500);
        body.put("temperature", 0.7);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        ResponseEntity<GroqResponse> response = restTemplate.exchange(
                GROQ_URL, HttpMethod.POST,
                new HttpEntity<>(body, headers),
                GroqResponse.class
        );

        if (response.getBody() != null
                && response.getBody().getChoices() != null
                && !response.getBody().getChoices().isEmpty()) {
            return response.getBody().getChoices().get(0).getMessage().getContent();
        }
        return "";
    }

    /**
     * Main method: build prompt + call Groq API → return reply string
     */
    @Transactional(readOnly = true)
    public String chat(AiChatRequest req) {
        String systemPrompt = buildSystemPrompt(req.getContext());

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt));

        if (req.getMessages() != null) {
            List<AiChatRequest.AiMessage> history = req.getMessages();
            int start = Math.max(0, history.size() - 6);
            for (AiChatRequest.AiMessage m : history.subList(start, history.size())) {
                messages.add(Map.of("role", m.getRole(), "content", m.getContent()));
            }
        }

        List<String> keys = getApiKeys();
        if (keys.isEmpty()) throw new RuntimeException("No API keys configured");

        for (int i = 0; i < keys.size(); i++) {
            try {
                log.info("Trying Groq key #{}", i + 1);
                String reply = callGroq(messages, keys.get(i));
                log.info("Success with key #{}", i + 1);
                return reply;
            } catch (HttpClientErrorException.TooManyRequests e) {
                log.warn("Key #{} hit rate limit (429), trying next...", i + 1);
            } catch (HttpClientErrorException e) {
                if (e.getStatusCode().value() == 401) {
                    log.warn("Key #{} invalid (401), trying next...", i + 1);
                } else {
                    throw e;
                }
            }
        }
        throw new RuntimeException("All API keys exhausted. Please try again later.");
    }

    // ─── Build System Prompt ─────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private String buildSystemPrompt(Map<String, Object> context) {
        StringBuilder sb = new StringBuilder();

        sb.append("""
                You are a friendly AI assistant for MovieZone — an online movie streaming platform.
                Your job is to help users find movies, answer questions about Premium plans,
                and provide information about content on the platform.
                
                IMPORTANT LANGUAGE RULE: Always respond in the SAME language the user writes in.
                - If user writes in English → reply in English
                - If user writes in Vietnamese → reply in Vietnamese
                - If user writes in Japanese → reply in Japanese
                - Never switch languages unless the user does first.
                
                Response rules:
                - Be friendly, concise and easy to understand.
                - Use appropriate emojis to make responses lively.
                - Use **bold** to highlight important information.
                - If the question is about movies, suggest specific titles from the available list.
                - If the question is about Premium, explain benefits and list available plans.
                - Do not make up information outside the provided data.
                - Always encourage users to sign up / upgrade to Premium when appropriate.
                - When user asks about an actor/director, search through the cast/director data below.
                
                """);

        // ── Movies from DB ──────────────────────────────────────────────────
        List<Movie> movies = movieRepository.findAllPublishedWithDetails();
        if (!movies.isEmpty()) {
            sb.append("## MOVIES on MovieZone:\n");

            for (Movie m : movies) {

                sb.append(String.format("- **%s** (Movie%s%s)\n",
                        m.getTitle(),
                        m.getReleaseDate() != null ? ", " + m.getReleaseDate().getYear() : "",
                        m.getVoteAverage() != null ? ", ⭐" + String.format("%.1f", m.getVoteAverage()) : ""
                ));

                if (m.getOverview() != null && !m.getOverview().isBlank()) {
                    String overview = m.getOverview().length() > 80
                            ? m.getOverview().substring(0, 80) + "..."
                            : m.getOverview();
                    sb.append("  Overview: ").append(overview).append("\n");
                }

                if (m.getGenres() != null && !m.getGenres().isEmpty()) {
                    String genres = m.getGenres().stream()
                            .map(Genre::getName)
                            .collect(Collectors.joining(", "));
                    sb.append("  Genres: ").append(genres).append("\n");
                }

                if (m.getCredits() != null && !m.getCredits().isEmpty()) {
                    List<String> actors = new ArrayList<>();
                    List<String> directors = new ArrayList<>();

                    m.getCredits().stream()
                            .sorted(Comparator.comparingInt(
                                    c -> c.getCreditOrder() == null ? 999 : c.getCreditOrder()))
                            .limit(7)
                            .forEach(credit -> {
                                if (credit.getPerson() == null) return;
                                String name = credit.getPerson().getName();
                                String job = credit.getJob();
                                if ("Director".equalsIgnoreCase(job)) {
                                    directors.add(name);
                                } else {
                                    actors.add(name);
                                }
                            });

                    if (!directors.isEmpty()) {
                        sb.append("  Director: ").append(String.join(", ", directors)).append("\n");
                    }
                    if (!actors.isEmpty()) {
                        sb.append("  Cast: ").append(String.join(", ", actors)).append("\n");
                    }
                }
                sb.append("\n");
            }
        }

        // ── TV Series from DB ───────────────────────────────────────────────
        List<TvSeries> tvList = tvSeriesRepository.findAllPublishedWithDetails();
        if (!tvList.isEmpty()) {
            sb.append("## TV SERIES on MovieZone:\n");
            for (TvSeries tv : tvList) {
                sb.append(String.format("- **%s** (TV Series%s%s)\n",
                        tv.getName(),
                        tv.getFirstAirDate() != null ? ", " + tv.getFirstAirDate().getYear() : "",
                        tv.getVoteAverage() != null ? ", ⭐" + String.format("%.1f", tv.getVoteAverage()) : ""
                ));

                if (tv.getOverview() != null && !tv.getOverview().isBlank()) {
                    String overview = tv.getOverview().length() > 80
                            ? tv.getOverview().substring(0, 80) + "..."
                            : tv.getOverview();
                    sb.append("  Overview: ").append(overview).append("\n");
                }

                if (tv.getGenres() != null && !tv.getGenres().isEmpty()) {
                    String genres = tv.getGenres().stream()
                            .map(Genre::getName)
                            .collect(Collectors.joining(", "));
                    sb.append("  Genres: ").append(genres).append("\n");
                }

                if (tv.getCredits() != null && !tv.getCredits().isEmpty()) {
                    List<String> actors = tv.getCredits().stream()
                            .sorted(Comparator.comparingInt(
                                    c -> c.getCreditOrder() == null ? 999 : c.getCreditOrder()))
                            .filter(c -> c.getPerson() != null)
                            .limit(8)
                            .map(c -> c.getPerson().getName())
                            .collect(Collectors.toList());

                    if (!actors.isEmpty()) {
                        sb.append("  Cast: ").append(String.join(", ", actors)).append("\n");
                    }
                }
                sb.append("\n");
            }
        }

        // ── Premium Packs (từ frontend context) ────────────────────────────
        if (context != null) {
            Object packsObj = context.get("packs");
            if (packsObj instanceof List<?> packsList && !packsList.isEmpty()) {
                sb.append("## Available Premium plans:\n");
                for (Object p : packsList) {
                    Map<String, Object> pack = (Map<String, Object>) p;
                    String name = String.valueOf(pack.getOrDefault("packName", ""));
                    Object price = pack.get("packPrice");
                    Object duration = pack.get("durationDays");
                    sb.append(String.format("- **%s**: %s VND / %s days\n",
                            name,
                            price != null ? formatPrice(price.toString()) : "N/A",
                            duration != null ? duration : "30"
                    ));
                }
                sb.append("Premium benefits: Ad-free movies, high speed, exclusive content.\n\n");
            }

            // User profile nếu có
            Object profileObj = context.get("userProfile");
            if (profileObj instanceof Map<?, ?>) {
                Map<String, Object> profile = (Map<String, Object>) profileObj;
                String username = String.valueOf(profile.getOrDefault("username", ""));
                if (!username.isBlank() && !"null".equals(username)) {
                    sb.append("## Current user: ").append(username).append("\n\n");
                }
            }
        }

        sb.append("""
                ## MovieZone Platform Guide:
                
                ### Contact Admin:
                - Go to the **Contact** page (navbar → Contact)
                - Fill in: Your Name, Email, Phone Number, Message
                - You can attach image files if needed
                - Click **SEND CONTACT** to submit
                - You will receive a confirmation email automatically
                
                ### Profile Page Features (navbar → Profile):
                - **User Information**: View and update your username
                - **History**: See your recently watched movies/episodes with progress
                - **Subscriptions**: View your active Premium plans, extend subscription
                - **My Wishlist**: Movies/series you saved to watch later
                - **Change Password**: Update your account password (need current password)
                - **Logout**: Sign out of your account
                
                ### Other Features:
                - **Search**: Use the search bar on the homepage to find movies by title
                - **Go Premium**: Click "Go Premium" in navbar to see subscription plans
                - **Movie Detail**: Click any movie card to see full details, cast, trailer
                - **Watch History**: Progress is auto-saved, resume anytime
                
                """);

        sb.append("""
                Instructions:
                - When user asks about an actor (e.g. "movies with Leonardo DiCaprio"),
                  search the Cast fields above and list matching movies.
                - When user asks about a director, search the Director fields above.
                - When user asks about a genre (e.g. "action movies"), filter by Genres above.
                - When user asks about trending/hot movies, suggest the highest rated ones from the list.
                - If no specific information is available, guide the user to use the search feature on MovieZone.
                - Never invent movie titles, actor names, or director names not listed above.
                """);

        return sb.toString();
    }

    private String formatPrice(String price) {
        try {
            long amount = (long) Double.parseDouble(price);
            return String.format("%,d", amount).replace(',', '.');
        } catch (Exception e) {
            return price;
        }
    }
}