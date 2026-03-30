package com.example.superapp.controller;

import com.example.superapp.dto.WishlistItemDto;
import com.example.superapp.dto.WishlistToggleRequest;
import com.example.superapp.dto.WishlistToggleResponse;
import com.example.superapp.service.WishlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/user/wishlist")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class WishlistController {

    private final WishlistService wishlistService;

    @GetMapping
    public ResponseEntity<List<WishlistItemDto>> getWishlist(@RequestParam long profileId) {
        return ResponseEntity.ok(wishlistService.getWishlist(profileId));
    }

    @PostMapping("/toggle")
    public ResponseEntity<WishlistToggleResponse> toggle(
            @RequestBody WishlistToggleRequest req) {
        return ResponseEntity.ok(wishlistService.toggle(req));
    }

    @GetMapping("/check")
    public ResponseEntity<WishlistToggleResponse> check(
            @RequestParam Long contentId,
            @RequestParam String contentType,
            @RequestParam long profileId) {
        return ResponseEntity.ok(wishlistService.check(profileId, contentId, contentType));
    }

    public static class ProfileRequest {
        public long profileId;
    }
}