package com.example.superapp.controller;

import com.example.superapp.entity.AdminLogs;
import com.example.superapp.entity.Banner;
import com.example.superapp.service.AdminLogsService;
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

    private final BannerService bannerService;

    private final AdminLogsService adminLogsService;

    //left-id: 2
    @PostMapping("/upload/left")
    public void replaceLeftBanner(@RequestParam("file")MultipartFile file,
                                  @RequestParam String toURL) throws IOException {
        Banner left = bannerService.getBannerById(2);
        byte[] resizedImage = ImageUtil.resizeImage(file, 160, 600);
        left.setData(resizedImage);
        left.setToURL(toURL);
        bannerService.save(left);
        adminLogsService.saveLog(new AdminLogs("Left banner is updated"));
    }

    //right-id: 3
    @PostMapping("/upload/right")
    public void replaceRightBanner(@RequestParam("file")MultipartFile file,
                                  @RequestParam String toURL) throws IOException {
        Banner right = bannerService.getBannerById(3);
        byte[] resizedImage = ImageUtil.resizeImage(file, 160, 600);
        right.setData(resizedImage);
        right.setToURL(toURL);
        bannerService.save(right);
        adminLogsService.saveLog(new AdminLogs("Right banner is updated"));

    }

    //bottomm-id: 1
    @PostMapping("/upload/bottom")
    public void replaceBottomBanner(@RequestParam("file")MultipartFile file,
                                  @RequestParam String toURL) throws IOException {
        Banner bottom = bannerService.getBannerById(1);
        byte[] resizedImage = ImageUtil.resizeImage(file, 728, 90);
        bottom.setData(resizedImage);
        bottom.setToURL(toURL);
        bannerService.save(bottom);
        adminLogsService.saveLog(new AdminLogs("Bottom banner is updated"));

    }
}
