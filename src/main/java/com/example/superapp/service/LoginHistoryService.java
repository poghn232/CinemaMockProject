package com.example.superapp.service;

import com.example.superapp.entity.LoginHistory;
import com.example.superapp.entity.User;
import com.example.superapp.repository.LoginHistoryRepository;
import com.example.superapp.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class LoginHistoryService {

    private final LoginHistoryRepository loginHistoryRepository;
    private final UserRepository userRepository;

    public LoginHistoryService(LoginHistoryRepository loginHistoryRepository,
                               UserRepository userRepository) {
        this.loginHistoryRepository = loginHistoryRepository;
        this.userRepository = userRepository;
    }

    public void saveLoginHistory(String username, String ipAddress, String region) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));

        String role = user.getRole() == null ? "" : user.getRole().toUpperCase();

        // Chỉ lưu với CUSTOMER
        if (!role.contains("CUSTOMER")) {
            return;
        }

        String countryCode = extractCountryCode(region);

        LoginHistory history = new LoginHistory(
                user,
                username,
                ipAddress,
                region,
                countryCode,
                LocalDateTime.now()
        );

        loginHistoryRepository.save(history);
    }

    public List<LoginHistory> getUserLoginHistory(String username) {
        return loginHistoryRepository.findByUsernameOrderByLoginTimeDesc(username);
    }

    public List<Object[]> countLoginByCountry() {
        return loginHistoryRepository.countLoginByCountry();
    }

    public List<Object[]> countLoginByRegion() {
        return loginHistoryRepository.countLoginByRegion();
    }

    private String extractCountryCode(String region) {
        if (region == null || region.isBlank()) {
            return "UNKNOWN";
        }

        // Ví dụ region dạng VN-HN thì countryCode = VN
        String[] parts = region.split("-");
        return parts.length > 0 ? parts[0].toUpperCase() : "UNKNOWN";
    }
}