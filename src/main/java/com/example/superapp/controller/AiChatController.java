package com.example.superapp.controller;

import com.example.superapp.dto.AiChatRequest;
import com.example.superapp.service.AiChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/ai-chat")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class AiChatController {

    private final AiChatService aiChatService;

    @PostMapping
    public ResponseEntity<Map<String, String>> chat(@RequestBody AiChatRequest req) {
        try {
            String reply = aiChatService.chat(req);
            return ResponseEntity.ok(Map.of("reply", reply));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("reply", "Sorry, an error occurred. Please try again! 😅"));
        }
    }
}