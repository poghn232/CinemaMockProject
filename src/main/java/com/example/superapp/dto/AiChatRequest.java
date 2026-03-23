package com.example.superapp.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
public class AiChatRequest {
    private List<AiMessage> messages;
    private Map<String, Object> context;

    @Getter
    @Setter
    public static class AiMessage {
        private String role;
        private String content;
    }
}