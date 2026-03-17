package com.example.superapp.utils;

import net.coobird.thumbnailator.Thumbnails;
import org.apache.commons.io.FileUtils;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class ImageUtil {
    public static byte[] imgToByteArray(String dest) {
        try {
            ClassPathResource resource = new ClassPathResource(dest);
            return FileUtils.readFileToByteArray(resource.getFile());
        } catch (IOException e) {
            System.err.println(e.getMessage());
            ;
            return null;
        }
    }

    public static byte[] resizeImage(MultipartFile image, int width, int height) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Thumbnails.of(image.getInputStream())
                  .size(width, height)
                  .keepAspectRatio(false)
                  .outputQuality(1)
                  .outputFormat("png")
                  .toOutputStream(outputStream);
        return outputStream.toByteArray();
    }
}
