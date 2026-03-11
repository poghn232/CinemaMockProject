package com.example.superapp.controller;

import com.example.superapp.entity.Banner;
import com.example.superapp.entity.Subscription;
import com.example.superapp.entity.User;
import com.example.superapp.service.BannerService;
import com.example.superapp.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class BannerController {

    private final BannerService bannerService;
    private final UserService userService;


    @GetMapping("/api/image/{id}")
    public ResponseEntity<byte[]> retrieveImage(@PathVariable int id, @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails != null) {
            User user = userService.getUserByUsername(userDetails.getUsername());
            Subscription lastSub = user.getSubscriptions().getLast();

            if (lastSub != null && lastSub.getEndDate().isAfter(LocalDateTime.now())) {
                return ResponseEntity.ok(new byte[0]);
            }
        }
        Banner banner = bannerService.getBannerById(id);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_PNG);
        headers.setContentLength(banner.getData().length);
        return new ResponseEntity<>(banner.getData(), headers, HttpStatus.OK);
    }

    @GetMapping("/api/banner")
    public ResponseEntity<List<Map<String, Object>>> retrieveAllBanners(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails != null) {
            User user = userService.getUserByUsername(userDetails.getUsername());
            Subscription lastSub = user.getSubscriptions().getLast();

            if (lastSub != null && lastSub.getEndDate().isAfter(LocalDateTime.now())) {
                return ResponseEntity.ok(Collections.emptyList());
            }
        }
        List<Banner> banners = bannerService.getAllBanners();
        List<Map<String, Object>> result = banners.stream().map(banner -> {
                                                      Map<String, Object> map = new HashMap<>();
                                                      map.put("id", banner.getBannerId());
                                                      map.put("toURL", banner.getToURL());
                                                      return map;
                                                  })
                                                  .toList();
        return ResponseEntity.ok(result);
    }

    // for temporary image upload
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
