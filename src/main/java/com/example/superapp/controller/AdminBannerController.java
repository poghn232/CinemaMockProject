package com.example.superapp.controller;

import com.example.superapp.entity.Banner;
import com.example.superapp.service.BannerService;
import com.example.superapp.utils.ImageUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/banner")
public class AdminBannerController {

    private final BannerService service;

    //left-id: 2
    @PostMapping("/upload/left")
    public void replaceLeftBanner(@RequestParam("file")MultipartFile file,
                                  @RequestParam String toURL) throws IOException {
        Banner left = service.getBannerById(2);
        byte[] resizedImage = ImageUtil.resizeImage(file, 160, 600);
        left.setData(resizedImage);
        left.setToURL(toURL);
        service.save(left);
    }

    //right-id: 3
    @PostMapping("/upload/right")
    public void replaceRightBanner(@RequestParam("file")MultipartFile file,
                                  @RequestParam String toURL) throws IOException {
        Banner right = service.getBannerById(3);
        byte[] resizedImage = ImageUtil.resizeImage(file, 160, 600);
        right.setData(resizedImage);
        right.setToURL(toURL);
        service.save(right);
    }

    //bottomm-id: 1
    @PostMapping("/upload/bottom")
    public void replaceBottomBanner(@RequestParam("file")MultipartFile file,
                                  @RequestParam String toURL) throws IOException {
        Banner bottom = service.getBannerById(1);
        byte[] resizedImage = ImageUtil.resizeImage(file, 728, 90);
        bottom.setData(resizedImage);
        bottom.setToURL(toURL);
        service.save(bottom);
    }
}
