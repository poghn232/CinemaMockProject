package com.example.superapp.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Component
public class FacebookTokenVerifier {

    private static final String GRAPH_URL =
            "https://graph.facebook.com/me?fields=id,name,email&access_token=";

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    public FacebookUserInfo verify(String accessToken) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(GRAPH_URL + accessToken))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(
                request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Facebook Graph API returned " + response.statusCode());
        }

        JsonNode node = mapper.readTree(response.body());

        if (node.has("error")) {
            throw new RuntimeException("Facebook token invalid: " + node.get("error").asText());
        }

        String id    = node.path("id").asText(null);
        String name  = node.path("name").asText(null);
        String email = node.path("email").asText(null);

        if (id == null) {
            throw new RuntimeException("Could not retrieve Facebook user ID");
        }

        return new FacebookUserInfo(id, name, email);
    }

    public record FacebookUserInfo(String id, String name, String email) {
        public String getId()    { return id; }
        public String getName()  { return name; }
        public String getEmail() { return email; }
    }
}