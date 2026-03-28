package com.example.superapp.controller;

import com.example.superapp.entity.Ad;
import com.example.superapp.repository.AdRepository;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public/ads")
@RequiredArgsConstructor
public class PublicAdController {

    private final AdRepository adRepository;

    @GetMapping("/pre-roll")
    public ResponseEntity<PublicAdResponse> getActivePreRollAd() {
        Ad ad = adRepository.findFirstByAdTypeAndStatusOrderByIdDesc("PRE_ROLL", "ACTIVE")
                .orElse(null);

        if (ad == null || ad.getSrcFilm() == null || ad.getSrcFilm().isBlank()) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(
                PublicAdResponse.builder()
                        .id(ad.getId())
                        .title(ad.getTitle())
                        .srcFilm(ad.getSrcFilm())
                        .skippable(Boolean.TRUE.equals(ad.getSkippable()))
                        .skipAfterSeconds(ad.getSkipAfterSeconds() == null ? 0 : ad.getSkipAfterSeconds())
                        .build()
        );
    }

    @Data
    @Builder
    public static class PublicAdResponse {
        private Long id;
        private String title;
        private String srcFilm;
        private boolean skippable;
        private int skipAfterSeconds;
    }
}