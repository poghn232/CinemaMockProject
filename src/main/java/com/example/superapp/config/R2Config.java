package com.example.superapp.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.S3Client;

import java.net.URI;

@Configuration
public class R2Config {

    @Bean
    @ConfigurationProperties(prefix = "cloudflare.r2")
    public R2Properties r2Properties() {
        return new R2Properties();
    }

    @Bean
    public S3Client r2S3Client(R2Properties props) {
        return S3Client.builder()
                .endpointOverride(URI.create("https://" + props.getAccountId() + ".r2.cloudflarestorage.com"))
                .region(Region.of("auto"))
                .credentialsProvider(
                        StaticCredentialsProvider.create(
                                AwsBasicCredentials.create(props.getAccessKey(), props.getSecretKey())
                        )
                )
                .serviceConfiguration(
                        S3Configuration.builder()
                                .chunkedEncodingEnabled(false)
                                .pathStyleAccessEnabled(false)
                                .build()
                )
                .build();
    }

    @Getter
    @Setter
    public static class R2Properties {
        private String accountId;
        private String accessKey;
        private String secretKey;
        private String bucket;
        private String publicBaseUrl;
    }
}