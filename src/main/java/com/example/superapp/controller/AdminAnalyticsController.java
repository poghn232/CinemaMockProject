package com.example.superapp.controller;

import com.example.superapp.entity.Subscription;
import com.example.superapp.entity.SubscriptionStatus;
import com.example.superapp.entity.User;
import com.example.superapp.entity.Genre;
import com.example.superapp.entity.Movie;
import com.example.superapp.repository.LoginHistoryRepository;
import com.example.superapp.repository.UserRepository;
import com.example.superapp.repository.SubscriptionRepository;
import com.example.superapp.repository.MovieRepository;
import com.example.superapp.repository.WatchHistoryRepository;
import com.example.superapp.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/analytics")
@RequiredArgsConstructor
public class AdminAnalyticsController {

    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final MovieRepository movieRepository;
    private final LoginHistoryRepository loginHistoryRepository;
    private final WatchHistoryRepository watchHistoryRepository;
    private final PaymentRepository paymentRepository;

    @GetMapping("/users")
    @Transactional(readOnly = true)
    public Map<String, Object> userStats(@RequestParam(defaultValue = "all") String period) {
        List<User> allUsers = userRepository.findAll();

        // Exclude admin accounts — only count regular users
        List<User> regularUsers = allUsers.stream()
                .filter(u -> u.getRole() == null || !u.getRole().trim().toUpperCase().contains("ADMIN"))
                .collect(Collectors.toList());

        List<Subscription> allSubs = subscriptionRepository.findAll();
        LocalDateTime cutoff = getCutoff(period);

        // Subscribed count: only among regular users
        Set<Long> regularUserIds = regularUsers.stream()
                .map(User::getUserId)
                .collect(Collectors.toSet());

        Set<Long> subscribedUserIds = allSubs.stream()
                .filter(s -> s.getStatus() != null && s.getStatus() == SubscriptionStatus.ACTIVE)
                .map(s -> s.getUser().getUserId())
                .filter(regularUserIds::contains)
                .collect(Collectors.toSet());

        long newUsers;
        if (cutoff == null) {
            newUsers = regularUsers.size();
        } else {
            newUsers = allSubs.stream()
                    .filter(s -> s.getStartDate() != null && s.getStartDate().isAfter(cutoff))
                    .map(s -> s.getUser().getUserId())
                    .filter(regularUserIds::contains)
                    .distinct()
                    .count();
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalUsers", regularUsers.size());
        result.put("subscribedUsers", subscribedUserIds.size());
        result.put("newUsers", newUsers);
        result.put("period", period);
        return result;
    }

    /**
     * Returns the most watched genres based on actual user watch history. Each
     * watch history entry (movie or TV episode) contributes 1 count to every
     * genre that content belongs to. Movies: WatchHistory → Movie → genres TV:
     * WatchHistory → Episode → Season → TvSeries → genres
     */
    @GetMapping("/genres")
    @Transactional(readOnly = true)
    public Map<String, Object> genreStats(@RequestParam(defaultValue = "all") String period) {
        LocalDateTime cutoff = getCutoff(period);

        List<com.example.superapp.entity.WatchHistory> history = watchHistoryRepository.findAll()
                .stream()
                // only include entries with a valid watchedAt (if cutoff applied) and at least one target (movie or episode)
                .filter(wh -> (cutoff == null || (wh.getWatchedAt() != null && wh.getWatchedAt().isAfter(cutoff)))
                && (wh.getMovie() != null || wh.getEpisode() != null))
                .toList();

        Map<String, Long> genreCounts = new LinkedHashMap<>();

        for (var wh : history) {
            // Movie watch
            if (wh.getMovie() != null) {
                var genres = wh.getMovie().getGenres();
                if (genres != null) {
                    for (Genre g : genres) {
                        if (g != null && g.getName() != null) {
                            genreCounts.merge(g.getName(), 1L, Long::sum);
                        }
                    }
                }
            }
            // TV episode watch → season → tvSeries → genres
            if (wh.getEpisode() != null
                    && wh.getEpisode().getSeason() != null
                    && wh.getEpisode().getSeason().getTvSeries() != null) {
                var tvGenres = wh.getEpisode().getSeason().getTvSeries().getGenres();
                if (tvGenres != null) {
                    for (Genre g : tvGenres) {
                        if (g != null && g.getName() != null) {
                            genreCounts.merge(g.getName(), 1L, Long::sum);
                        }
                    }
                }
            }
        }

        // Sort descending, top 10 + Others
        List<Map.Entry<String, Long>> sorted = genreCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .toList();

        List<String> labels = new ArrayList<>();
        List<Long> values = new ArrayList<>();
        long othersTotal = 0;

        for (int i = 0; i < sorted.size(); i++) {
            if (i < 10) {
                labels.add(sorted.get(i).getKey());
                values.add(sorted.get(i).getValue());
            } else {
                othersTotal += sorted.get(i).getValue();
            }
        }
        if (othersTotal > 0) {
            labels.add("Others");
            values.add(othersTotal);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("labels", labels);
        result.put("values", values);
        result.put("totalWatches", history.size());
        result.put("period", period);
        return result;
    }

    /**
     * Returns popularity counts for subscription packs. Uses only ACTIVE
     * subscriptions and supports the same 'period' cutoff as other endpoints.
     */
    @GetMapping("/subpacks")
    @Transactional(readOnly = true)
    public Map<String, Object> subscriptionPackStats(@RequestParam(defaultValue = "all") String period) {
        LocalDateTime cutoff = getCutoff(period);

        List<Subscription> subs = subscriptionRepository.findAll().stream()
                .filter(s -> s.getPack() != null)
                // count active subscriptions only
                .filter(s -> s.getStatus() != null && s.getStatus() == SubscriptionStatus.ACTIVE)
                // respect period cutoff if provided
                .filter(s -> cutoff == null || (s.getStartDate() != null && s.getStartDate().isAfter(cutoff)))
                .toList();

        Map<String, Long> packCounts = new LinkedHashMap<>();
        for (var s : subs) {
            var p = s.getPack();
            if (p != null && p.getPackName() != null) {
                packCounts.merge(p.getPackName(), 1L, Long::sum);
            }
        }

        List<Map.Entry<String, Long>> sorted = packCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .toList();

        List<String> labels = new ArrayList<>();
        List<Long> values = new ArrayList<>();
        long othersTotal = 0;

        for (int i = 0; i < sorted.size(); i++) {
            if (i < 10) {
                labels.add(sorted.get(i).getKey());
                values.add(sorted.get(i).getValue());
            } else {
                othersTotal += sorted.get(i).getValue();
            }
        }
        if (othersTotal > 0) {
            labels.add("Others");
            values.add(othersTotal);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("labels", labels);
        result.put("values", values);
        result.put("totalSubscriptions", subs.size());
        result.put("period", period);
        return result;
    }

    @GetMapping("/revenue")
    @Transactional(readOnly = true)
    public Map<String, Object> revenueStats() {
        // Sum only completed payments
        var payments = paymentRepository.findAll();
        java.math.BigDecimal total = payments.stream()
                .filter(p -> p.getStatus() != null && p.getStatus() == com.example.superapp.entity.PaymentStatus.SUCCESS)
                .map(p -> p.getAmount() == null ? java.math.BigDecimal.ZERO : p.getAmount())
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("totalRevenue", total);
        return res;
    }

    @GetMapping("/logins")
    @Transactional(readOnly = true)
    public Map<String, Object> loginStats(@RequestParam(defaultValue = "all") String period) {
        LocalDateTime cutoff = getCutoff(period);

        Set<String> adminUsernames = userRepository.findAll()
                .stream()
                .filter(u -> u.getRole() != null && u.getRole().toUpperCase().contains("ADMIN"))
                .map(User::getUsername)
                .collect(Collectors.toSet());

        // Filter and group all login records by country code
        var filteredLogins = loginHistoryRepository.findAll()
                .stream()
                .filter(lh -> cutoff == null || (lh.getLoginTime() != null && lh.getLoginTime().isAfter(cutoff)))
                .filter(lh -> lh.getCountryCode() != null && !lh.getCountryCode().isBlank())
                .toList();

        // Total count per region
        Map<String, Long> totalByRegion = new LinkedHashMap<>();
        for (var lh : filteredLogins) {
            totalByRegion.merge(lh.getCountryCode().toUpperCase(), 1L, Long::sum);
        }

        // Admin count per region
        Map<String, Long> adminByRegion = filteredLogins.stream()
                .filter(lh -> adminUsernames.contains(lh.getUsername()))
                .collect(Collectors.groupingBy(
                        lh -> lh.getCountryCode().toUpperCase(), Collectors.counting()));

        // Sort by total descending
        List<Map.Entry<String, Long>> sorted = totalByRegion.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .toList();

        List<String> labels = new ArrayList<>();
        List<Long> totalValues = new ArrayList<>();
        List<Long> adminValues = new ArrayList<>();
        List<Long> userValues = new ArrayList<>();
        long othersTotal = 0, othersAdmin = 0;

        long totalLogins = filteredLogins.size();

        for (int i = 0; i < sorted.size(); i++) {
            String code = sorted.get(i).getKey();
            long count = sorted.get(i).getValue();
            long adminCount = adminByRegion.getOrDefault(code, 0L);
            if (i < 10) {
                labels.add(code);
                totalValues.add(count);
                adminValues.add(adminCount);
                userValues.add(count - adminCount);
            } else {
                othersTotal += count;
                othersAdmin += adminCount;
            }
        }
        if (othersTotal > 0) {
            labels.add("Others");
            totalValues.add(othersTotal);
            adminValues.add(othersAdmin);
            userValues.add(othersTotal - othersAdmin);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("labels", labels);
        result.put("totalValues", totalValues);
        result.put("adminValues", adminValues);
        result.put("userValues", userValues);
        result.put("totalLogins", totalLogins);
        result.put("period", period);
        return result;
    }

    private LocalDateTime getCutoff(String period) {
        return switch (period.toLowerCase()) {
            case "weekly" ->
                LocalDateTime.now().minusWeeks(1);
            case "monthly" ->
                LocalDateTime.now().minusMonths(1);
            default ->
                null;
        };
    }
}