//TODO: update with profile
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
import lombok.RequiredArgsConstructor;
import com.example.superapp.repository.PaymentRepository;
import com.example.superapp.entity.Payment;
import com.example.superapp.entity.PaymentStatus;
import java.math.BigDecimal;
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
    private final PaymentRepository paymentRepository;
    private final MovieRepository movieRepository;
    private final LoginHistoryRepository loginHistoryRepository;

    @GetMapping("/users")
    @Transactional(readOnly = true)
    public Map<String, Object> userStats(@RequestParam(defaultValue = "all") String period) {
        List<User> allUsers = userRepository.findAll()
                .stream()
                .filter(u -> u.getRole() == null || !u.getRole().toUpperCase().contains("ADMIN"))
                .toList();

        List<Subscription> allSubs = subscriptionRepository.findAll();
        LocalDateTime cutoff = getCutoff(period);

        Set<Long> subscribedUserIds = allSubs.stream()
                .filter(s -> s.getStatus() != null && s.getStatus() == SubscriptionStatus.ACTIVE)
                .map(s -> s.getUser().getUserId())
                .collect(Collectors.toSet());

        long newUsers;
        if (cutoff == null) {
            newUsers = allUsers.size();
        } else {
            newUsers = allSubs.stream()
                    .filter(s -> s.getStartDate() != null && s.getStartDate().isAfter(cutoff))
                    .map(s -> s.getUser().getUserId())
                    .distinct()
                    .count();
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalUsers", allUsers.size());
        result.put("subscribedUsers", subscribedUserIds.size());
        result.put("newUsers", newUsers);
        result.put("period", period);
        return result;
    }

    @GetMapping("/genres")
    @Transactional(readOnly = true)
    public Map<String, Object> genreStats(@RequestParam(defaultValue = "all") String period) {
        List<Movie> movies = movieRepository.findAll()
                .stream()
                .filter(m -> Boolean.TRUE.equals(m.getActive()))
                .toList();

        Map<String, Long> genreCounts = new LinkedHashMap<>();
        for (Movie movie : movies) {
            long weight = movie.getVoteCount() != null ? movie.getVoteCount() : 1L;
            for (Genre g : movie.getGenres()) {   // genres is LAZY — needs open session
                genreCounts.merge(g.getName(), weight, Long::sum);
            }
        }

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
        result.put("period", period);
        return result;
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

    @GetMapping("/revenue")
    public Map<String, Object> revenue(@RequestParam(defaultValue = "all") String period) {
        // Sum amounts for payments with SUCCESS status. Period parameter is accepted for future extension.
        List<Payment> payments = paymentRepository.findAll();
        BigDecimal total = payments.stream()
                .filter(p -> p.getStatus() != null && p.getStatus() == PaymentStatus.SUCCESS)
                .map(Payment::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalRevenue", total); // BigDecimal serializes nicely with Jackson
    result.put("currency", "VND");
        result.put("period", period);
        return result;
    }
}
