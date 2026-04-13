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
    public LoginStreak recordLogin(Profile profile) {
        LocalDate today = LocalDate.now();

        LoginStreak streak = loginStreakRepository.findByProfile(profile)
                .orElseGet(() -> LoginStreak.builder()
                        .profile(profile)
                        .currentStreak(0)
                        .longestStreak(0)
                        .totalLoginDays(0)
                        .build());

        LocalDate last = streak.getLastLoginDate();

        if (today.equals(last)) return streak;

        if (last != null && last.plusDays(1).equals(today)) {
            streak.setCurrentStreak(streak.getCurrentStreak() + 1);
        } else {
            streak.setCurrentStreak(1);
        }

        streak.setLastLoginDate(today);
        streak.setTotalLoginDays(streak.getTotalLoginDays() + 1);

        if (streak.getCurrentStreak() > streak.getLongestStreak()) {
            streak.setLongestStreak(streak.getCurrentStreak());
        }

        LoginStreak saved = loginStreakRepository.save(streak);
        checkStreakAchievements(profile, saved.getCurrentStreak());

        log.info("[Streak] profile={} | current={} longest={} total={}",
                profile.getProfileName(),
                saved.getCurrentStreak(),
                saved.getLongestStreak(),
                saved.getTotalLoginDays());
        return saved;
    }

    private void checkStreakAchievements(Profile profile, int current) {
        if (current >= 3)   achievementService.grant(profile, "STREAK_3");
        if (current >= 7)   achievementService.grant(profile, "STREAK_7");
        if (current >= 14)  achievementService.grant(profile, "STREAK_14");
        if (current >= 30)  achievementService.grant(profile, "STREAK_30");
        if (current >= 100) achievementService.grant(profile, "STREAK_100");
    }

    @Transactional(readOnly = true)
    public LoginStreak getStreak(Profile profile) {
        return loginStreakRepository.findByProfile(profile)
                .orElseGet(() -> LoginStreak.builder()
                        .profile(profile)
                        .currentStreak(0)
                        .longestStreak(0)
                        .totalLoginDays(0)
                        .build());
    }
}