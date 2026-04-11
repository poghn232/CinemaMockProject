package com.example.superapp.controller;

import com.example.superapp.dto.WishlistItemDto;
import com.example.superapp.dto.WishlistToggleRequest;
import com.example.superapp.dto.WishlistToggleResponse;
import com.example.superapp.entity.User;
import com.example.superapp.repository.UserRepository;
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
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<List<WishlistItemDto>> getWishlist(
            @RequestParam(value = "profileId", required = false) Long profileId,
            Authentication auth,
            jakarta.servlet.http.HttpServletRequest request) {
        Long pid = resolveProfileId(auth, profileId);
        return ResponseEntity.ok(wishlistService.getWishlist(pid, request));
    }

    @PostMapping("/toggle")
    public ResponseEntity<WishlistToggleResponse> toggle(
            @RequestBody WishlistToggleRequest req, Authentication auth) {
        Long pid = req.profileId() != null ? req.profileId() : resolveProfileId(auth, null);
        return ResponseEntity.ok(wishlistService.toggle(pid, req));
    }

    @GetMapping("/check")
    public ResponseEntity<WishlistToggleResponse> check(
            @RequestParam(value = "profileId", required = false) Long profileId,
            @RequestParam Long contentId,
            @RequestParam String contentType,
            Authentication auth) {
        Long pid = resolveProfileId(auth, profileId);
        return ResponseEntity.ok(wishlistService.check(pid, contentId, contentType));
    }

    private Long resolveProfileId(Authentication auth, Long profileId) {
        if (profileId != null) return profileId;
        User user = userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (user.getProfiles() == null || user.getProfiles().isEmpty()) {
            throw new RuntimeException("User has no profile");
        }
        return user.getProfiles().get(0).getProfileId();
    }
}