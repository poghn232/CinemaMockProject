package com.example.superapp.controller;

import com.example.superapp.entity.Subscription;
import com.example.superapp.entity.SubscriptionStatus;
import com.example.superapp.entity.User;
import com.example.superapp.repository.SubscriptionRepository;
import com.example.superapp.repository.UserRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") 
public class UserController {

    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final PasswordEncoder passwordEncoder;

    // 1. LẤY THÔNG TIN PROFILE VÀ CÁC GÓI SUBSCRIPTION (AN TOÀN)
    @GetMapping("/profile")
    public ResponseEntity<?> profile(Authentication authentication) {
        String username = authentication.getName(); 
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // TRÍCH XUẤT DỮ LIỆU SUBSCRIPTION (Tránh lỗi đệ quy vòng lặp vô hạn của JPA)
        List<Map<String, Object>> subscriptionList = user.getSubscriptions().stream()
                .map(sub -> {
                    Map<String, Object> subMap = new HashMap<>();
                    subMap.put("subId", sub.getSubId());
                    // Lấy trạng thái Enum chuyển thành String
                    subMap.put("status", sub.getStatus() != null ? sub.getStatus().name() : "PENDING");
                    subMap.put("startDate", sub.getStartDate());
                    subMap.put("endDate", sub.getEndDate());

                    // Trích xuất thông tin gói (Pack)
                    if (sub.getPack() != null) {
                        Map<String, Object> packMap = new HashMap<>();
                        packMap.put("packId", sub.getPack().getPackId());
                        packMap.put("packName", sub.getPack().getPackName());
                        packMap.put("packPrice", sub.getPack().getPackPrice());
                        packMap.put("durationDays", sub.getPack().getDurationDays());
                        packMap.put("maxProfiles", sub.getPack().getMaxProfiles());
                        subMap.put("pack", packMap);
                    }
                    return subMap;
                }).toList();

        return ResponseEntity.ok(Map.of(
                "userId", user.getUserId(),
                "username", user.getUsername(),
                "email", user.getEmail(),
                "role", user.getRole(),
                "subscriptions", subscriptionList // Trả về danh sách đã được format chuẩn
        ));
    }

    // 2. CẬP NHẬT PROFILE (USERNAME)
    @Data
    public static class ProfileUpdateRequest {
        private String username;
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(Authentication authentication, @RequestBody ProfileUpdateRequest request) {
        String currentUsername = authentication.getName();
        User user = userRepository.findByUsername(currentUsername).orElseThrow(() -> new RuntimeException("User not found"));

        Optional<User> existingUser = userRepository.findByUsername(request.getUsername());
        if (existingUser.isPresent() && !existingUser.get().getUserId().equals(user.getUserId())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Username này đã có người sử dụng!"));
        }

        user.setUsername(request.getUsername());
        userRepository.save(user);

        return ResponseEntity.ok(Map.of("message", "Cập nhật thông tin thành công!"));
    }

    // 3. XỬ LÝ NÚT THANH TOÁN / HỦY GÓI TRÊN GIAO DIỆN
    @PostMapping("/subscription/{subId}/{action}")
    public ResponseEntity<?> handleSubscriptionAction(Authentication authentication, @PathVariable Long subId, @PathVariable String action) {
        String username = authentication.getName();
        User user = userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found"));

        Subscription sub = subscriptionRepository.findById(subId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy gói đăng ký"));

        if (!sub.getUser().getUserId().equals(user.getUserId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Không có quyền thao tác!"));
        }

        if ("COMPLETE".equals(action) || "RETRY".equals(action)) {
            sub.setStatus(SubscriptionStatus.ACTIVE);
            sub.setStartDate(LocalDateTime.now());
            int duration = sub.getPack().getDurationDays() != null ? sub.getPack().getDurationDays() : 30;
            sub.setEndDate(LocalDateTime.now().plusDays(duration));
        } else if ("CANCEL_REQ".equals(action)) {
            sub.setStatus(SubscriptionStatus.CANCELLED);
        } else {
            return ResponseEntity.badRequest().body(Map.of("message", "Hành động không hợp lệ!"));
        }

        subscriptionRepository.save(sub);
        return ResponseEntity.ok(Map.of("message", "Thao tác gói đăng ký thành công!"));
    }

    // 4. ĐỔI MẬT KHẨU
    @Data
    public static class PasswordUpdateRequest {
        private String oldPassword;
        private String newPassword;
    }

    @PutMapping("/password")
    public ResponseEntity<?> updatePassword(Authentication authentication, @RequestBody PasswordUpdateRequest request) {
        String username = authentication.getName();
        User user = userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "Mật khẩu hiện tại không chính xác!"));
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        return ResponseEntity.ok(Map.of("message", "Đổi mật khẩu thành công!"));
    }
}