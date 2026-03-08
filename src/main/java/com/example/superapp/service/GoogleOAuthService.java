package com.example.superapp.service;

import com.example.superapp.entity.User;
import com.example.superapp.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class GoogleOAuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public GoogleOAuthService(UserRepository userRepository,
                              PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User processGoogleUser(String email, String fullName) {
        return userRepository.findByEmail(email)
                .orElseGet(() -> createGoogleUser(email, fullName));
    }

    private User createGoogleUser(String email, String fullName) {
        String baseUsername = extractUsernameFromEmail(email);
        String finalUsername = generateUniqueUsername(baseUsername);

        User user = new User();
        user.setEmail(email);
        user.setUsername(finalUsername);

        // account Google không dùng password local thật, nhưng entity đang bắt buộc phải có
        user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));

        user.setRole("CUSTOMER");
        user.setEnabled(true);

        return userRepository.save(user);
    }

    private String extractUsernameFromEmail(String email) {
        int atIndex = email.indexOf("@");
        if (atIndex <= 0) {
            throw new RuntimeException("Email không hợp lệ");
        }
        return email.substring(0, atIndex).trim().toLowerCase();
    }

    private String generateUniqueUsername(String baseUsername) {
        String candidate = baseUsername;
        int counter = 1;

        while (userRepository.existsByUsername(candidate)) {
            candidate = baseUsername + counter;
            counter++;
        }
        return candidate;
    }
}