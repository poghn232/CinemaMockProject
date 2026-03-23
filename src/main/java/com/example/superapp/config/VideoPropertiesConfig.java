package com.example.superapp.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class VideoPropertiesConfig {

    @Bean
    @ConfigurationProperties(prefix = "app.video")
    public VideoProperties videoProperties() {
        return new VideoProperties();
    }

    @Getter
    @Setter
    public static class VideoProperties {
        private String uploadDir;
        private String workDir;
        private String publicBaseUrl;
        private int hlsSegmentSeconds = 6;

        // 👉 THÊM 2 DÒNG NÀY
        private String ffmpegPath;
        private String ffprobePath;
    }
}