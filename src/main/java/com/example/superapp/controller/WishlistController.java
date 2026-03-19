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
    public ResponseEntity<List<WishlistItemDto>> getWishlist(Authentication auth, jakarta.servlet.http.HttpServletRequest request) {
        return ResponseEntity.ok(wishlistService.getWishlist(auth.getName(), request));
    }

    @PostMapping("/toggle")
    public ResponseEntity<WishlistToggleResponse> toggle(
            @RequestBody WishlistToggleRequest req, Authentication auth) {
        return ResponseEntity.ok(wishlistService.toggle(auth.getName(), req));
    }

    @GetMapping("/check")
    public ResponseEntity<WishlistToggleResponse> check(
            @RequestParam Long contentId,
            @RequestParam String contentType,
            Authentication auth) {
        return ResponseEntity.ok(wishlistService.check(auth.getName(), contentId, contentType));
    }
}