package com.example.superapp.service;

import com.example.superapp.config.VideoPropertiesConfig.VideoProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VideoEncodingService {

    private final VideoProperties videoProperties;

    public EncodeResult encodeToHlsAdaptive(Path inputFile, Long videoAssetId, Integer sourceHeight) {
        try {
            String ffmpegPath = videoProperties.getFfmpegPath();
            if (ffmpegPath == null || ffmpegPath.isBlank()) {
                throw new IllegalStateException("app.video.ffmpeg-path is empty");
            }

            Path workRoot = Paths.get(videoProperties.getWorkDir()).toAbsolutePath().normalize();
            Files.createDirectories(workRoot);

            Path outputDir = workRoot.resolve("asset-" + videoAssetId);
            if (Files.exists(outputDir)) {
                deleteRecursively(outputDir);
            }
            Files.createDirectories(outputDir);

            List<Variant> variants = buildVariants(sourceHeight);
            if (variants.isEmpty()) {
                throw new IllegalStateException("No valid encode variants for source");
            }

            // Tạo sẵn thư mục cho từng variant để HLS ghi file không bị lỗi
            for (int i = 0; i < variants.size(); i++) {
                Files.createDirectories(outputDir.resolve("v" + i));
            }

            List<String> command = new ArrayList<>();
            command.add(ffmpegPath);
            command.add("-y");

            // Nếu muốn thử tăng tốc decode bằng GPU, có thể mở 2 dòng dưới.
            // Nhưng một số máy/driver sẽ kén input, nên mặc định để an toàn là decode bằng CPU.
            // command.add("-hwaccel");
            // command.add("cuda");

            command.add("-i");
            command.add(inputFile.toAbsolutePath().toString());

            // Map video/audio cho từng variant
            for (int i = 0; i < variants.size(); i++) {
                command.add("-map");
                command.add("0:v:0");
                command.add("-map");
                command.add("0:a:0?");
            }

            // Audio apply cho toàn bộ output streams
            command.add("-c:a");
            command.add("aac");
            command.add("-ar");
            command.add("48000");
            command.add("-b:a");
            command.add("128k");

            for (int i = 0; i < variants.size(); i++) {
                Variant v = variants.get(i);

                // GPU encode bằng NVENC
                command.add("-c:v:" + i);
                command.add("h264_nvenc");

                // p1 nhanh nhất -> p7 đẹp nhất, p4/p5 là mức cân bằng
                command.add("-preset:v:" + i);
                command.add("p4");

                // main/high đều được, main an toàn hơn
                command.add("-profile:v:" + i);
                command.add("main");

                // rate control
                command.add("-rc:v:" + i);
                command.add("vbr");

                command.add("-b:v:" + i);
                command.add(v.videoBitrate);

                command.add("-maxrate:v:" + i);
                command.add(v.maxRate);

                command.add("-bufsize:v:" + i);
                command.add(v.bufSize);

                // keyframe ổn định cho HLS
                command.add("-sc_threshold:v:" + i);
                command.add("0");

                command.add("-g:v:" + i);
                command.add("180");

                command.add("-keyint_min:v:" + i);
                command.add("180");

                // scale theo từng độ phân giải
                command.add("-vf:" + i);
                command.add("scale=w=-2:h=" + v.height);
            }

            command.add("-f");
            command.add("hls");

            command.add("-hls_time");
            command.add(String.valueOf(videoProperties.getHlsSegmentSeconds()));

            command.add("-hls_playlist_type");
            command.add("vod");

            command.add("-hls_flags");
            command.add("independent_segments");

            command.add("-hls_segment_filename");
            command.add(outputDir.resolve("v%v").resolve("seg_%03d.ts").toString());

            command.add("-master_pl_name");
            command.add("master.m3u8");

            StringBuilder varStreamMap = new StringBuilder();
            for (int i = 0; i < variants.size(); i++) {
                if (i > 0) varStreamMap.append(" ");
                varStreamMap.append("v:").append(i).append(",a:").append(i);
            }

            command.add("-var_stream_map");
            command.add(varStreamMap.toString());

            command.add(outputDir.resolve("v%v").resolve("playlist.m3u8").toString());

            System.out.println("Using ffmpeg path = " + ffmpegPath);
            System.out.println("Encoding input = " + inputFile.toAbsolutePath());
            System.out.println("Encoding outputDir = " + outputDir.toAbsolutePath());
            System.out.println("FFmpeg command = " + String.join(" ", command));

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);

            Process process = pb.start();

            StringBuilder logs = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = br.readLine()) != null) {
                    logs.append(line).append("\n");
                    System.out.println(line);
                }
            }

            int exit = process.waitFor();
            if (exit != 0) {
                throw new RuntimeException("FFmpeg failed with exit code " + exit + ":\n" + logs);
            }

            return new EncodeResult(outputDir, variants);
        } catch (Exception e) {
            throw new RuntimeException("Encode failed: " + e.getMessage(), e);
        }
    }

    private List<Variant> buildVariants(Integer sourceHeight) {
        List<Variant> all = List.of(
                new Variant("360p", 360, "800k", "1200k", "1600k"),
                new Variant("720p", 720, "2800k", "4200k", "5600k"),
                new Variant("1080p", 1080, "5000k", "7500k", "10000k")
        );

        List<Variant> result = new ArrayList<>();
        for (Variant v : all) {
            if (sourceHeight == null || sourceHeight >= v.height) {
                result.add(v);
            }
        }

        if (result.isEmpty() && sourceHeight != null) {
            result.add(new Variant(sourceHeight + "p", sourceHeight, "800k", "1200k", "1600k"));
        }

        return result;
    }

    private void deleteRecursively(Path path) throws IOException {
        if (!Files.exists(path)) return;

        Files.walk(path)
                .sorted((a, b) -> b.getNameCount() - a.getNameCount())
                .forEach(p -> {
                    try {
                        Files.deleteIfExists(p);
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to delete " + p, e);
                    }
                });
    }

    public record Variant(String name, int height, String videoBitrate, String maxRate, String bufSize) {}
    public record EncodeResult(Path outputDir, List<Variant> variants) {}
}