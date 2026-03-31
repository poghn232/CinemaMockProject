package com.example.superapp.service;

import com.example.superapp.entity.*;
import com.example.superapp.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoginStreakService {

    private final LoginStreakRepository loginStreakRepository;
    private final AchievementService achievementService;

    @Transactional
    public LoginStreak recordLogin(User user) {
        LocalDate today = LocalDate.now();

        LoginStreak streak = loginStreakRepository.findByUser(user)
                .orElseGet(() -> LoginStreak.builder()
                        .user(user)
                        .currentStreak(0)
                        .longestStreak(0)
                        .totalLoginDays(0)
                        .build());

        LocalDate last = streak.getLastLoginDate();

        // Đã đăng nhập hôm nay rồi → không làm gì
        if (today.equals(last)) {
            return streak;
        }

        // Đăng nhập ngày hôm qua → tiếp tục streak
        if (last != null && last.plusDays(1).equals(today)) {
            streak.setCurrentStreak(streak.getCurrentStreak() + 1);
        } else {
            // Bỏ ngày hoặc lần đầu → reset
            streak.setCurrentStreak(1);
        }

        streak.setLastLoginDate(today);
        streak.setTotalLoginDays(streak.getTotalLoginDays() + 1);

        // Cập nhật longest streak
        if (streak.getCurrentStreak() > streak.getLongestStreak()) {
            streak.setLongestStreak(streak.getCurrentStreak());
        }

        LoginStreak saved = loginStreakRepository.save(streak);

        // Check streak achievements
        checkStreakAchievements(user, saved.getCurrentStreak());

        log.info("[Streak] {} | current={} longest={} total={}",
                user.getUsername(),
                saved.getCurrentStreak(),
                saved.getLongestStreak(),
                saved.getTotalLoginDays());
        log.info("Last login: {}, Today: {}", last, today);
        return saved;
    }

    private void checkStreakAchievements(User user, int current) {
        if (current >= 3)   achievementService.grant(user, "STREAK_3");
        if (current >= 7)   achievementService.grant(user, "STREAK_7");
        if (current >= 14)  achievementService.grant(user, "STREAK_14");
        if (current >= 30)  achievementService.grant(user, "STREAK_30");
        if (current >= 100) achievementService.grant(user, "STREAK_100");
    }

    @Transactional(readOnly = true)
    public LoginStreak getStreak(User user) {
        return loginStreakRepository.findByUser(user)
                .orElseGet(() -> LoginStreak.builder()
                        .user(user)
                        .currentStreak(0)
                        .longestStreak(0)
                        .totalLoginDays(0)
                        .build());
    }
}