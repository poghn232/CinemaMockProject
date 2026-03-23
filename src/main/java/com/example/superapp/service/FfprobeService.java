package com.example.superapp.service;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Path;

@Service
public class FfprobeService {

    public ProbeResult probe(Path videoPath) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "ffprobe",
                    "-v", "error",
                    "-select_streams", "v:0",
                    "-show_entries", "stream=width,height",
                    "-show_entries", "format=duration",
                    "-of", "default=noprint_wrappers=1:nokey=1",
                    videoPath.toAbsolutePath().toString()
            );

            Process process = pb.start();

            try (BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String widthLine = br.readLine();
                String heightLine = br.readLine();
                String durationLine = br.readLine();

                int exit = process.waitFor();
                if (exit != 0) {
                    throw new RuntimeException("ffprobe failed");
                }

                Integer width = widthLine != null ? Integer.parseInt(widthLine.trim()) : null;
                Integer height = heightLine != null ? Integer.parseInt(heightLine.trim()) : null;
                Integer duration = durationLine != null ? (int) Math.round(Double.parseDouble(durationLine.trim())) : null;

                return new ProbeResult(width, height, duration);
            }
        } catch (Exception e) {
            throw new RuntimeException("Cannot probe video: " + e.getMessage(), e);
        }
    }

    @Data
    @AllArgsConstructor
    public static class ProbeResult {
        private Integer width;
        private Integer height;
        private Integer durationSeconds;
    }
}