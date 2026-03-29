package com.example.superapp.controller;

import com.example.superapp.dto.CreateFromR2Request;
import com.example.superapp.dto.VideoAssetDto;
import com.example.superapp.entity.Ad;
import com.example.superapp.repository.AdRepository;
import com.example.superapp.service.VideoAssetService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/ads")
@RequiredArgsConstructor
public class AdminAdController {

    private final AdRepository adRepository;
    private final VideoAssetService videoAssetService;

    @GetMapping
    public ResponseEntity<List<Ad>> getAllAds() {
        return ResponseEntity.ok(adRepository.findAll());
    }

    @GetMapping("/{adId}")
    public ResponseEntity<Ad> getAdById(@PathVariable Long adId) {
        Ad ad = adRepository.findById(adId)
                .orElseThrow(() -> new IllegalArgumentException("Ad not found: " + adId));
        return ResponseEntity.ok(ad);
    }
    @PostMapping("/create-from-r2")
    public ResponseEntity<Ad> createAdFromR2(@RequestBody CreateFromR2Request request) {
        if (request.getSourceAdId() == null) {
            throw new IllegalArgumentException("sourceAdId is required");
        }

        String playbackUrl = videoAssetService.findPlaybackUrlFromExistingR2("ad", request.getSourceAdId())
                .orElseThrow(() -> new IllegalArgumentException("No R2 asset found for sourceAdId: " + request.getSourceAdId()));

        Ad ad = Ad.builder()
                .title(request.getTitle())
                .adType(request.getAdType() == null || request.getAdType().isBlank() ? "PRE_ROLL" : request.getAdType())
                .status(request.getStatus() == null || request.getStatus().isBlank() ? "INACTIVE" : request.getStatus())
                .srcFilm(playbackUrl)
                .skippable(request.getSkippable() != null ? request.getSkippable() : false)
                .skipAfterSeconds(request.getSkipAfterSeconds())
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(adRepository.save(ad));
    }

    @PostMapping
    public ResponseEntity<Ad> createAd(@RequestBody CreateAdRequest request) {
        Ad ad = Ad.builder()
                .title(request.getTitle())
                .adType(request.getAdType() == null || request.getAdType().isBlank() ? "PRE_ROLL" : request.getAdType())
                .status(request.getStatus() == null || request.getStatus().isBlank() ? "INACTIVE" : request.getStatus())
                .srcFilm(null)
                .skippable(request.getSkippable() != null ? request.getSkippable() : false)
                .skipAfterSeconds(request.getSkipAfterSeconds())
                .build();

        Ad saved = adRepository.save(ad);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @GetMapping("/api/public/ads/pre-roll")
    public ResponseEntity<?> getActivePreRollAd() {
        return adRepository.findFirstByAdTypeIgnoreCaseAndStatusIgnoreCase("PRE_ROLL", "ACTIVE")
                .map(ad -> ResponseEntity.ok(Map.of(
                        "id", ad.getId(),
                        "title", ad.getTitle(),
                        "srcFilm", ad.getSrcFilm(),
                        "skippable", Boolean.TRUE.equals(ad.getSkippable()),
                        "skipAfterSeconds", ad.getSkipAfterSeconds() == null ? 0 : ad.getSkipAfterSeconds()
                )))
                .orElse(ResponseEntity.noContent().build());
    }

    @PutMapping("/{adId}")
    public ResponseEntity<Ad> updateAd(
            @PathVariable Long adId,
            @RequestBody UpdateAdRequest request
    ) {
        Ad ad = adRepository.findById(adId)
                .orElseThrow(() -> new IllegalArgumentException("Ad not found: " + adId));

        if (request.getTitle() != null) {
            ad.setTitle(request.getTitle());
        }

        if (request.getAdType() != null && !request.getAdType().isBlank()) {
            ad.setAdType(request.getAdType());
        }

        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            ad.setStatus(request.getStatus());
        }

        if (request.getSkippable() != null) {
            ad.setSkippable(request.getSkippable());
        }

        if (request.getSkipAfterSeconds() != null) {
            ad.setSkipAfterSeconds(request.getSkipAfterSeconds());
        }

        Ad saved = adRepository.save(ad);
        return ResponseEntity.ok(saved);
    }

    @PostMapping("/{adId}/source")
    public ResponseEntity<VideoAssetDto> uploadAdSource(
            @PathVariable Long adId,
            @RequestParam("file") MultipartFile file
    ) {
        if (!adRepository.existsById(adId)) {
            throw new IllegalArgumentException("Ad not found: " + adId);
        }

        VideoAssetDto dto = videoAssetService.uploadSource("ad", adId, file);
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/{adId}/source/latest")
    public ResponseEntity<VideoAssetDto> getLatestAdAsset(@PathVariable Long adId) {
        if (!adRepository.existsById(adId)) {
            throw new IllegalArgumentException("Ad not found: " + adId);
        }

        VideoAssetDto dto = videoAssetService.getLatestAsset("ad", adId);
        return ResponseEntity.ok(dto);
    }

    @PutMapping("/{adId}/activate")
    public ResponseEntity<Ad> activateAd(@PathVariable Long adId) {
        Ad ad = adRepository.findById(adId)
                .orElseThrow(() -> new IllegalArgumentException("Ad not found: " + adId));

        if (ad.getSrcFilm() == null || ad.getSrcFilm().isBlank()) {
            throw new IllegalStateException("Cannot activate ad without playback URL");
        }

        ad.setStatus("ACTIVE");
        Ad saved = adRepository.save(ad);
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/{adId}/deactivate")
    public ResponseEntity<Ad> deactivateAd(@PathVariable Long adId) {
        Ad ad = adRepository.findById(adId)
                .orElseThrow(() -> new IllegalArgumentException("Ad not found: " + adId));

        ad.setStatus("INACTIVE");
        Ad saved = adRepository.save(ad);
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/{adId}/activate-exclusive")
    public ResponseEntity<Ad> activateAdExclusively(@PathVariable Long adId) {
        Ad target = adRepository.findById(adId)
                .orElseThrow(() -> new IllegalArgumentException("Ad not found: " + adId));

        List<Ad> allAds = adRepository.findAll();
        for (Ad ad : allAds) {
            if ("PRE_ROLL".equalsIgnoreCase(ad.getAdType())) {
                ad.setStatus(ad.getId().equals(adId) ? "ACTIVE" : "INACTIVE");
            }
        }

        adRepository.saveAll(allAds);

        Ad refreshed = adRepository.findById(adId)
                .orElseThrow(() -> new IllegalArgumentException("Ad not found after update: " + adId));

        return ResponseEntity.ok(refreshed);
    }
    @GetMapping("/{adId}/r2-status")
    public ResponseEntity<Map<String, Object>> getAdR2Status(@PathVariable Long adId) {
        if (!adRepository.existsById(adId)) {
            throw new IllegalArgumentException("Ad not found: " + adId);
        }

        Map<String, Object> result = videoAssetService.getR2Status("ad", adId);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{adId}/sync-from-r2")
    public ResponseEntity<Map<String, Object>> syncPlaybackFromR2(@PathVariable Long adId) {
        if (!adRepository.existsById(adId)) {
            throw new IllegalArgumentException("Ad not found: " + adId);
        }

        boolean synced = videoAssetService.syncExistingPlaybackFromR2AndReturn("ad", adId);

        return ResponseEntity.ok(Map.of(
                "success", synced,
                "ownerType", "ad",
                "ownerId", adId
        ));
    }

    @DeleteMapping("/{adId}")
    public ResponseEntity<Map<String, String>> deleteAd(@PathVariable Long adId) {
        Ad ad = adRepository.findById(adId)
                .orElseThrow(() -> new IllegalArgumentException("Ad not found: " + adId));

        adRepository.delete(ad);

        return ResponseEntity.ok(Map.of(
                "message", "Ad deleted successfully",
                "adId", String.valueOf(adId)
        ));
    }

    @Data
    public static class CreateAdRequest {
        private String title;
        private String adType; // PRE_ROLL
        private String status; // ACTIVE / INACTIVE
        private Boolean skippable;
        private Integer skipAfterSeconds;
    }

    @Data
    public static class UpdateAdRequest {
        private String title;
        private String adType;
        private String status;
        private Boolean skippable;
        private Integer skipAfterSeconds;
    }
}