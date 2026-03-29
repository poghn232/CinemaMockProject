package com.example.superapp.service;

import com.example.superapp.config.VideoPropertiesConfig.VideoProperties;
import com.example.superapp.dto.VideoAssetDto;
import com.example.superapp.entity.Ad;
import com.example.superapp.entity.Episode;
import com.example.superapp.entity.Movie;
import com.example.superapp.entity.VideoAsset;
import com.example.superapp.repository.AdRepository;
import com.example.superapp.repository.EpisodeRepository;
import com.example.superapp.repository.MovieRepository;
import com.example.superapp.repository.VideoAssetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class VideoAssetService {

    private final VideoAssetRepository videoAssetRepository;
    private final VideoProperties videoProperties;
    private final FfprobeService ffprobeService;
    private final VideoEncodingService videoEncodingService;
    private final R2StorageService r2StorageService;
    private final MovieRepository movieRepository;
    private final EpisodeRepository episodeRepository;
    private final AdRepository adRepository;

    public VideoAssetDto uploadSource(String ownerType, Long ownerId, MultipartFile file) {
        validateFile(file);

        try {
            Path uploadDir = Paths.get(videoProperties.getUploadDir());
            Files.createDirectories(uploadDir);

            String safeName = System.currentTimeMillis() + "_" + file.getOriginalFilename().replaceAll("[^a-zA-Z0-9._-]", "_");
            Path localPath = uploadDir.resolve(safeName);

            Files.copy(file.getInputStream(), localPath, StandardCopyOption.REPLACE_EXISTING);

            FfprobeService.ProbeResult probe = ffprobeService.probe(localPath);

            VideoAsset asset = VideoAsset.builder()
                    .ownerType(ownerType)
                    .ownerId(ownerId)
                    .originalFileName(file.getOriginalFilename())
                    .localSourcePath(localPath.toAbsolutePath().toString())
                    .fileSizeBytes(file.getSize())
                    .status("UPLOADED")
                    .progressPercent(5)
                    .durationSeconds(probe.getDurationSeconds())
                    .sourceWidth(probe.getWidth())
                    .sourceHeight(probe.getHeight())
                    .build();

            asset = videoAssetRepository.save(asset);

            processEncodeAsync(asset.getId());

            return toDto(asset);
        } catch (Exception e) {
            throw new RuntimeException("Upload source failed: " + e.getMessage(), e);
        }
    }

    @Async("videoTaskExecutor")
    public void processEncodeAsync(Long videoAssetId) {
        VideoAsset asset = videoAssetRepository.findById(videoAssetId)
                .orElseThrow(() -> new IllegalArgumentException("VideoAsset not found"));

        try {
            asset.setStatus("PROCESSING");
            asset.setProgressPercent(10);
            videoAssetRepository.save(asset);

            Path input = Paths.get(asset.getLocalSourcePath());

            VideoEncodingService.EncodeResult encodeResult =
                    videoEncodingService.encodeToHlsAdaptive(input, asset.getId(), asset.getSourceHeight());

            asset.setProgressPercent(65);
            videoAssetRepository.save(asset);

            String rootKey = buildRootObjectKey(asset);

            // upload master
            Path master = encodeResult.outputDir().resolve("master.m3u8");
            String masterKey = rootKey + "/master.m3u8";
            r2StorageService.uploadFile(master, masterKey, "application/vnd.apple.mpegurl");

            // upload variant playlists + segments
            for (VideoEncodingService.Variant variant : encodeResult.variants()) {
                int idx = encodeResult.variants().indexOf(variant);
                Path variantDir = encodeResult.outputDir().resolve("v" + idx);

                if (Files.exists(variantDir.resolve("playlist.m3u8"))) {
                    r2StorageService.uploadFile(
                            variantDir.resolve("playlist.m3u8"),
                            rootKey + "/v" + idx + "/playlist.m3u8",
                            "application/vnd.apple.mpegurl"
                    );
                }

                try (DirectoryStream<Path> ds = Files.newDirectoryStream(variantDir, "*.ts")) {
                    for (Path seg : ds) {
                        r2StorageService.uploadFile(
                                seg,
                                rootKey + "/v" + idx + "/" + seg.getFileName(),
                                "video/mp2t"
                        );
                    }
                }
            }

            asset.setMasterPlaylistKey(masterKey);
            asset.setPlaybackUrl(r2StorageService.buildPublicUrl(masterKey));
            asset.setStatus("READY");
            asset.setProgressPercent(100);

            List<VideoEncodingService.Variant> vs = encodeResult.variants();
            asset.setHas360p(vs.stream().anyMatch(v -> v.height() == 360));
            asset.setHas720p(vs.stream().anyMatch(v -> v.height() == 720));
            asset.setHas1080p(vs.stream().anyMatch(v -> v.height() == 1080));

            videoAssetRepository.save(asset);
            attachPlaybackUrlToOwner(asset, asset.getPlaybackUrl());

        } catch (Exception e) {
            asset.setStatus("FAILED");
            asset.setErrorMessage(e.getMessage());
            asset.setProgressPercent(0);
            videoAssetRepository.save(asset);
        }
    }

    public VideoAssetDto getLatestAsset(String ownerType, Long ownerId) {
        VideoAsset asset = videoAssetRepository
                .findTopByOwnerTypeAndOwnerIdOrderByCreatedAtDesc(ownerType, ownerId)
                .orElse(null);

        if (asset == null) {
            return VideoAssetDto.builder()
                    .ownerType(ownerType)
                    .ownerId(ownerId)
                    .status("NOT_UPLOADED")
                    .progressPercent(0)
                    .has360p(false)
                    .has720p(false)
                    .has1080p(false)
                    .build();
        }

        return toDto(asset);
    }

    public Map<String, Object> getR2Status(String ownerType, Long ownerId) {
        Optional<String> latestMasterKey = r2StorageService.findLatestMasterPlaylistKey(ownerType, ownerId);

        if (latestMasterKey.isEmpty()) {
            return Map.of(
                    "found", false,
                    "ownerType", ownerType,
                    "ownerId", ownerId,
                    "masterPlaylistKey", "",
                    "playbackUrl", ""
            );
        }

        String masterKey = latestMasterKey.get();
        String playbackUrl = r2StorageService.buildPublicUrl(masterKey);

        return Map.of(
                "found", true,
                "ownerType", ownerType,
                "ownerId", ownerId,
                "masterPlaylistKey", masterKey,
                "playbackUrl", playbackUrl
        );
    }

    public boolean syncExistingPlaybackFromR2AndReturn(String ownerType, Long ownerId) {
        Optional<String> latestMasterKey = r2StorageService.findLatestMasterPlaylistKey(ownerType, ownerId);

        if (latestMasterKey.isEmpty()) {
            return false;
        }

        String masterKey = latestMasterKey.get();
        String playbackUrl = r2StorageService.buildPublicUrl(masterKey);

        if ("ad".equalsIgnoreCase(ownerType)) {
            Ad ad = adRepository.findById(ownerId)
                    .orElseThrow(() -> new IllegalArgumentException("Ad not found: " + ownerId));
            ad.setSrcFilm(playbackUrl);
            adRepository.save(ad);
            return true;
        }

        if ("movie".equalsIgnoreCase(ownerType)) {
            Movie movie = movieRepository.findById(ownerId)
                    .orElseThrow(() -> new IllegalArgumentException("Movie not found: " + ownerId));
            movie.setSrcFilm(playbackUrl);
            movieRepository.save(movie);
            return true;
        }

        if ("tv_episode".equalsIgnoreCase(ownerType)) {
            Episode episode = episodeRepository.findById(ownerId)
                    .orElseThrow(() -> new IllegalArgumentException("Episode not found: " + ownerId));
            episode.setSrcFilm(playbackUrl);
            episodeRepository.save(episode);
            return true;
        }

        throw new IllegalArgumentException("Unsupported ownerType: " + ownerType);
    }

    private String buildRootObjectKey(VideoAsset asset) {
        return "videos/" + asset.getOwnerType() + "/" + asset.getOwnerId() + "/asset-" + asset.getId() + "/hls";
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        if (!name.endsWith(".mp4")) {
            throw new IllegalArgumentException("Only .mp4 is supported for now");
        }
    }

    private VideoAssetDto toDto(VideoAsset asset) {
        java.util.List<String> variants = java.util.Collections.emptyList();

        if (asset.getId() != null && asset.getOwnerType() != null && asset.getOwnerId() != null) {
            try {
                variants = r2StorageService.findVariants(
                        asset.getOwnerType(),
                        asset.getOwnerId(),
                        asset.getId()
                );
            } catch (Exception e) {
                variants = java.util.Collections.emptyList();
            }
        }

        return VideoAssetDto.builder()
                .id(asset.getId())
                .ownerType(asset.getOwnerType())
                .ownerId(asset.getOwnerId())
                .status(asset.getStatus())
                .progressPercent(asset.getProgressPercent())
                .playbackUrl(asset.getPlaybackUrl())
                .masterPlaylistKey(asset.getMasterPlaylistKey())
                .errorMessage(asset.getErrorMessage())
                .has360p(asset.getHas360p())
                .has720p(asset.getHas720p())
                .has1080p(asset.getHas1080p())
                .durationSeconds(asset.getDurationSeconds())
                .variants(variants)
                .build();
    }
    private void attachPlaybackUrlToOwner(VideoAsset asset, String playbackUrl) {
        if ("movie".equalsIgnoreCase(asset.getOwnerType())) {
            Movie movie = movieRepository.findById(asset.getOwnerId())
                    .orElseThrow(() -> new IllegalArgumentException("Movie not found: " + asset.getOwnerId()));
            movie.setSrcFilm(playbackUrl);
            movieRepository.save(movie);
            return;
        }

        if ("tv_episode".equalsIgnoreCase(asset.getOwnerType())) {
            Episode episode = episodeRepository.findById(asset.getOwnerId())
                    .orElseThrow(() -> new IllegalArgumentException("Episode not found: " + asset.getOwnerId()));
            episode.setSrcFilm(playbackUrl);
            episodeRepository.save(episode);
            return;
        }

        if ("ad".equalsIgnoreCase(asset.getOwnerType())) {
            Ad ad = adRepository.findById(asset.getOwnerId())
                    .orElseThrow(() -> new IllegalArgumentException("Ad not found: " + asset.getOwnerId()));
            ad.setSrcFilm(playbackUrl);
            adRepository.save(ad);
            return;
        }

        throw new IllegalArgumentException("Unsupported ownerType: " + asset.getOwnerType());
    }
    public void syncExistingPlaybackFromR2(String ownerType, Long ownerId) {
        r2StorageService.findLatestMasterPlaylistKey(ownerType, ownerId).ifPresent(masterKey -> {
            String playbackUrl = r2StorageService.buildPublicUrl(masterKey);

            if ("movie".equalsIgnoreCase(ownerType)) {
                Movie movie = movieRepository.findById(ownerId)
                        .orElseThrow(() -> new IllegalArgumentException("Movie not found: " + ownerId));
                movie.setSrcFilm(playbackUrl);
                movieRepository.save(movie);
                return;
            }

            if ("tv_episode".equalsIgnoreCase(ownerType)) {
                Episode episode = episodeRepository.findById(ownerId)
                        .orElseThrow(() -> new IllegalArgumentException("Episode not found: " + ownerId));
                episode.setSrcFilm(playbackUrl);
                episodeRepository.save(episode);
                return;
            }

            if ("ad".equalsIgnoreCase(ownerType)) {
                Ad ad = adRepository.findById(ownerId)
                        .orElseThrow(() -> new IllegalArgumentException("Ad not found: " + ownerId));
                ad.setSrcFilm(playbackUrl);
                adRepository.save(ad);
                return;
            }

            throw new IllegalArgumentException("Unsupported ownerType: " + ownerType);
        });
    }

    public Optional<String> findPlaybackUrlFromExistingR2(String ownerType, Long ownerId) {
        return r2StorageService.findLatestMasterPlaylistKey(ownerType, ownerId)
                .map(r2StorageService::buildPublicUrl);
    }

    public int backfillMovieSrcFilmFromR2() {
        List<Movie> movies = movieRepository.findAll();
        int updated = 0;

        for (Movie movie : movies) {
            String current = movie.getSrcFilm();
            if (current != null && !current.isBlank()) {
                continue;
            }

            boolean synced = syncExistingPlaybackFromR2AndReturn("movie", movie.getId());
            if (synced) {
                updated++;
            }
        }

        return updated;
    }

    public int backfillEpisodeSrcFilmFromR2() {
        List<Episode> episodes = episodeRepository.findAll();
        int updated = 0;

        for (Episode episode : episodes) {
            String current = episode.getSrcFilm();
            if (current != null && !current.isBlank()) {
                continue;
            }

            boolean synced = syncExistingPlaybackFromR2AndReturn("tv_episode", episode.getId());
            if (synced) {
                updated++;
            }
        }

        return updated;
    }



}