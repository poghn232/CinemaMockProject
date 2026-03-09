package com.example.superapp.controller;

import com.example.superapp.entity.Banner;
import com.example.superapp.service.BannerService;
import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;

@Controller
public class BannerController {

    @Autowired
    private BannerService bannerService;

    @GetMapping("/api/image/{id}")
    public ResponseEntity<byte[]> retrieveImage(@PathVariable int id) {
        Banner banner = bannerService.getBannerById(id);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_PNG);
        headers.setContentLength(banner.getData().length);
        System.out.println("Data length: " + banner.getData().length);

        return new ResponseEntity<>(banner.getData(), headers, HttpStatus.OK);
    }

    @PostMapping("/api/admin/banner/upload")
    public ResponseEntity<?> uploadBanner(@RequestParam("file") MultipartFile file,
                                          @RequestParam String tourl) throws IOException {
        Banner banner = new Banner();
        banner.setData(file.getBytes());
        banner.setToURL(tourl);
        bannerService.save(banner);
        return ResponseEntity.ok("Uploaded!");
    }
}
