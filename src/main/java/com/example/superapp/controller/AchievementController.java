package com.example.superapp.controller;

import com.example.superapp.entity.Achievement;
import com.example.superapp.entity.User;
import com.example.superapp.entity.UserAchievement;
import com.example.superapp.repository.UserRepository;
import com.example.superapp.service.AchievementService;
import com.example.superapp.service.LoginStreakService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/user/achievements")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AchievementController {

    private final AchievementService achievementService;
    private final UserRepository userRepository;
    private final LoginStreakService loginStreakService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAchievements(Authentication auth) {
        User user = userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<UserAchievement> earned = achievementService.getUserAchievements(user);
        List<Achievement> all = achievementService.getAllAchievements();

        Set<String> earnedCodes = earned.stream()
                .map(ua -> ua.getAchievement().getCode())
                .collect(Collectors.toSet());

        List<Map<String, Object>> earnedList = earned.stream().map(ua -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("code",        ua.getAchievement().getCode());
            m.put("name",        ua.getAchievement().getName());
            m.put("description", ua.getAchievement().getDescription());
            m.put("icon",        ua.getAchievement().getIcon());
            m.put("rarity",      ua.getAchievement().getRarity());
            m.put("earnedAt",    ua.getEarnedAt()
                    .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
            return m;
        }).collect(Collectors.toList());

        List<Map<String, Object>> lockedList = all.stream()
                .filter(a -> !earnedCodes.contains(a.getCode()))
                .map(a -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("code",        a.getCode());
                    m.put("name",        a.getName());
                    m.put("description", a.getDescription());
                    m.put("icon",        a.getIcon());
                    m.put("rarity",      a.getRarity());
                    m.put("locked",      true);
                    return m;
                }).collect(Collectors.toList());

        return ResponseEntity.ok(Map.of(
                "earned", earnedList,
                "locked", lockedList,
                "total",  all.size(),
                "count",  earned.size()
        ));
    }

    @GetMapping("/streak")
    public ResponseEntity<Map<String, Object>> getStreak(Authentication auth) {
        User user = userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        var streak = loginStreakService.getStreak(user);

        return ResponseEntity.ok(Map.of(
                "currentStreak",  streak.getCurrentStreak(),
                "longestStreak",  streak.getLongestStreak(),
                "totalLoginDays", streak.getTotalLoginDays(),
                "lastLoginDate",  streak.getLastLoginDate() != null
                        ? streak.getLastLoginDate().toString() : ""
        ));
    }
}