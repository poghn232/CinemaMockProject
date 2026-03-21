package com.example.superapp.service;

import com.example.superapp.config.R2Config.R2Properties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

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


    public String buildPublicUrl(String objectKey) {
        String base = r2Properties.getPublicBaseUrl();
        if (base.endsWith("/")) {
            return base + objectKey;
        }
        return base + "/" + objectKey;
    }
}