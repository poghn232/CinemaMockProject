package com.example.superapp.utils;

import org.apache.commons.io.FileUtils;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;

public class ImageUtil {
    public static byte[] imgToByteArray(String dest) {
        try {
            ClassPathResource resource = new ClassPathResource(dest);
            return FileUtils.readFileToByteArray(resource.getFile());
        } catch (IOException e) {
            System.err.println(e.getMessage());;
            return null;
        }
    }
}
