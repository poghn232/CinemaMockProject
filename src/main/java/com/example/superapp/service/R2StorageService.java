package com.example.superapp.service;

import com.example.superapp.config.R2Config.R2Properties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.core.sync.ResponseTransformer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class R2StorageService {

    private final S3Client r2S3Client;
    private final R2Properties r2Properties;


    public void uploadFile(Path localFile, String objectKey, String contentType) throws IOException {
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(r2Properties.getBucket())
                .key(objectKey)
                .contentType(contentType)
                .build();

        r2S3Client.putObject(request, RequestBody.fromBytes(Files.readAllBytes(localFile)));
    }

    public void deleteFile(String objectKey) {
        DeleteObjectRequest request = DeleteObjectRequest.builder()
                .bucket(r2Properties.getBucket())
                .key(objectKey)
                .build();

        r2S3Client.deleteObject(request);
    }

    public void deleteAllFilesInBucket() {
        String bucket = r2Properties.getBucket();
        String continuationToken = null;

        do {
            ListObjectsV2Request.Builder listBuilder = ListObjectsV2Request.builder()
                    .bucket(bucket);

            if (continuationToken != null) {
                listBuilder.continuationToken(continuationToken);
            }

            ListObjectsV2Response response = r2S3Client.listObjectsV2(listBuilder.build());

            for (S3Object object : response.contents()) {
                System.out.println("Deleting object: " + object.key());
                deleteFile(object.key());
            }

            continuationToken = response.nextContinuationToken();
        } while (continuationToken != null);
    }

    public void createFolder(String folderKey) {
        String key = folderKey.endsWith("/") ? folderKey : folderKey + "/";

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(r2Properties.getBucket())
                .key(key)
                .contentLength(0L)
                .build();

        r2S3Client.putObject(request, RequestBody.fromBytes(new byte[0]));
    }


    public String buildPublicUrl(String objectKey) {
        String base = r2Properties.getPublicBaseUrl();
        if (base.endsWith("/")) {
            return base + objectKey;
        }
        return base + "/" + objectKey;
    }

    public boolean objectExists(String objectKey) {
        try {
            HeadObjectRequest request = HeadObjectRequest.builder()
                    .bucket(r2Properties.getBucket())
                    .key(objectKey)
                    .build();
            r2S3Client.headObject(request);
            return true;
        } catch (S3Exception e) {
            return false;
        }
    }

    public Optional<String> findLatestMasterPlaylistKey(String ownerType, Long ownerId) {
        String prefix = "videos/" + ownerType + "/" + ownerId + "/";

        ListObjectsV2Request request = ListObjectsV2Request.builder()
                .bucket(r2Properties.getBucket())
                .prefix(prefix)
                .build();

        ListObjectsV2Response response = r2S3Client.listObjectsV2(request);

        return response.contents().stream()
                .map(S3Object::key)
                .filter(key -> key.endsWith("/hls/master.m3u8"))
                .max(Comparator.comparing(this::extractAssetIdSafely));
    }

    public java.util.List<String> findVariants(String ownerType, Long ownerId, Long assetId) {
        String prefix = "videos/" + ownerType + "/" + ownerId + "/asset-" + assetId + "/hls/";

        ListObjectsV2Request request = ListObjectsV2Request.builder()
                .bucket(r2Properties.getBucket())
                .prefix(prefix)
                .build();

        ListObjectsV2Response response = r2S3Client.listObjectsV2(request);

        return response.contents().stream()
                .map(S3Object::key)
                .map(key -> key.substring(prefix.length()))
                .filter(rest -> rest.matches("v\\d+/playlist\\.m3u8"))
                .map(rest -> rest.substring(0, rest.indexOf("/")))
                .distinct()
                .sorted(java.util.Comparator.comparingInt(v -> {
                    try {
                        return Integer.parseInt(v.substring(1));
                    } catch (Exception e) {
                        return Integer.MAX_VALUE;
                    }
                }))
                .toList();
    }

    public java.util.List<String> listObjectsByPrefix(String prefix) {
        ListObjectsV2Request request = ListObjectsV2Request.builder()
                .bucket(r2Properties.getBucket())
                .prefix(prefix)
                .build();

        ListObjectsV2Response response = r2S3Client.listObjectsV2(request);

        return response.contents().stream().map(S3Object::key).toList();
    }

    public java.util.Optional<ResponseBytes<GetObjectResponse>> getObjectBytes(String objectKey) {
        try {
            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(r2Properties.getBucket())
                    .key(objectKey)
                    .build();

            ResponseBytes<GetObjectResponse> resp = r2S3Client.getObject(request, ResponseTransformer.toBytes());
            return java.util.Optional.ofNullable(resp);
        } catch (Exception e) {
            return java.util.Optional.empty();
        }
    }

    private long extractAssetIdSafely(String key) {
        try {
            int idx = key.indexOf("/asset-");
            if (idx < 0) return -1L;

            int start = idx + "/asset-".length();
            int end = key.indexOf("/", start);
            if (end < 0) return -1L;

            return Long.parseLong(key.substring(start, end));
        } catch (Exception e) {
            return -1L;
        }
    }

    public java.util.List<String> findVariantsWithoutDb(String ownerType, Long ownerId) {
        String basePrefix = "videos/" + ownerType + "/" + ownerId + "/";

        ListObjectsV2Request request = ListObjectsV2Request.builder()
                .bucket(r2Properties.getBucket())
                .prefix(basePrefix)
                .build();

        ListObjectsV2Response response = r2S3Client.listObjectsV2(request);

        // 👉 tìm assetId lớn nhất (asset mới nhất)
        Optional<Long> latestAssetId = response.contents().stream()
                .map(S3Object::key)
                .map(this::extractAssetIdSafely)
                .filter(id -> id > 0)
                .max(Long::compareTo);

        if (latestAssetId.isEmpty()) {
            return java.util.Collections.emptyList();
        }

        Long assetId = latestAssetId.get();

        // 👉 reuse lại logic cũ
        return findVariants(ownerType, ownerId, assetId);
    }
    public Optional<String> findPlaybackUrlWithoutDb(String ownerType, Long ownerId) {
        return findLatestMasterPlaylistKey(ownerType, ownerId)
                .map(this::buildPublicUrl);
    }
}