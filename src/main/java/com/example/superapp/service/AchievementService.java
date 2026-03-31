package com.example.superapp.service;

import com.example.superapp.entity.*;
import com.example.superapp.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AchievementService {

    private final AchievementRepository achievementRepository;
    private final UserAchievementRepository userAchievementRepository;
    private final UserRepository userRepository;
    private final WatchHistoryRepository watchHistoryRepository;
    private final ReviewRepository reviewRepository;
    private final WishlistRepository wishlistRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final NotificationRepository notificationRepository;

    // ── Seed achievements on startup ────────────────────────────────────────
    @PostConstruct
    public void seedAchievements() {
        upsert("FIRST_WATCH",     "First Watch",       "Watch your first movie or episode",         "🎬", "COMMON");
        upsert("WATCH_10",        "Movie Enthusiast",  "Watch 10 movies or episodes",               "🍿", "COMMON");
        upsert("WATCH_50",        "Cinema Addict",     "Watch 50 movies or episodes",               "🎭", "RARE");
        upsert("WATCH_100",       "Century Club",      "Watch 100 movies or episodes",              "💯", "EPIC");
        upsert("FIRST_COMMENT",   "First Review",      "Write your first comment",                  "✍️", "COMMON");
        upsert("COMMENT_5",       "Critic",            "Write 5 comments",                          "⭐", "RARE");
        upsert("COMMENT_20",      "Top Reviewer",      "Write 20 comments",                         "🏆", "EPIC");
        upsert("PREMIUM_MEMBER",  "Premium Member",    "Subscribe to a Premium plan",               "💎", "RARE");
        upsert("GENRE_EXPLORER",  "Genre Explorer",    "Watch content from 5 different genres",     "🌍", "RARE");
        upsert("NIGHT_OWL",       "Night Owl",         "Watch something after midnight",            "🦉", "COMMON");
        upsert("BINGE_WATCHER",   "Binge Watcher",     "Watch 5 episodes of the same series in one day", "🔥", "EPIC");
        upsert("WISHLIST_10",     "Curator",           "Add 10 items to your wishlist",             "📌", "COMMON");
        upsert("STREAK_3",   "Consistent",     "Login 3 days in a row",   "🔥",  "COMMON");
        upsert("STREAK_7",   "Weekly Warrior", "Login 7 days in a row",   "⚡",  "RARE");
        upsert("STREAK_14",  "Dedicated",      "Login 14 days in a row",  "💪",  "EPIC");
        upsert("STREAK_30",  "Unstoppable",    "Login 30 days in a row",  "🚀",  "LEGENDARY");
        upsert("STREAK_100", "Legend",         "Login 100 days in a row", "👑",  "LEGENDARY");
    }

    private void upsert(String code, String name, String desc, String icon, String rarity) {
        if (achievementRepository.findByCode(code).isEmpty()) {
            achievementRepository.save(Achievement.builder()
                    .code(code).name(name).description(desc)
                    .icon(icon).rarity(rarity).build());
        }
    }

    // ── Grant achievement (safe: skips if already earned) ───────────────────
    @Transactional
    public boolean grant(User user, String code) {
        if (userAchievementRepository.existsByUserAndAchievement_Code(user, code)) {
            return false;
        }
        Achievement achievement = achievementRepository.findByCode(code).orElse(null);
        if (achievement == null) return false;

        userAchievementRepository.save(UserAchievement.builder()
                .user(user).achievement(achievement).build());

        // 🔥 TẠO NOTIFICATION
        Notification notif = Notification.builder()
                .user(user)
                .contentId(0L)
                .contentType("achievement")
                .contentTitle(achievement.getName()) // ✅ FIX

                .messageKey("notif.achievement_unlock")
                .messageParams(buildAchievementParams(achievement.getName()))// ✅ FIX

                .iconUrl(achievement.getIcon())
                .posterUrl(null)

                .eventType("ACHIEVEMENT_UNLOCK")
                .isRead(false)
                .build();

        notificationRepository.save(notif);

        log.info("[Achievement] {} earned '{}' ({})", user.getUsername(), achievement.getName(), code);
        return true;
    }

    private String buildAchievementParams(String name) {
        try {
            return new ObjectMapper().writeValueAsString(
                    Map.of("name", name)
            );
        } catch (Exception e) {
            return "{}";
        }
    }

    private String buildParams(String title, String episodeName) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper()
                    .writeValueAsString(
                            java.util.Map.of(
                                    "title", title,
                                    "episode", episodeName != null ? episodeName : ""
                            )
                    );
        } catch (Exception e) {
            return "{}";
        }
    }

    // ── Check all watch-based achievements ──────────────────────────────────
    @Transactional
    public void checkWatchAchievements(User user) {
        long totalWatched = watchHistoryRepository.findByUser_UserIdOrderByWatchedAtDesc(user.getUserId()).size();

        if (totalWatched >= 1)   grant(user, "FIRST_WATCH");
        if (totalWatched >= 10)  grant(user, "WATCH_10");
        if (totalWatched >= 50)  grant(user, "WATCH_50");
        if (totalWatched >= 100) grant(user, "WATCH_100");

        checkNightOwl(user);
        checkBingeWatcher(user);
        checkGenreExplorer(user);
    }

    // ── Check all comment-based achievements ────────────────────────────────
    @Transactional
    public void checkCommentAchievements(User user) {
        long totalComments = reviewRepository.findByUser_UserId(user.getUserId()).size();

        if (totalComments >= 1)  grant(user, "FIRST_COMMENT");
        if (totalComments >= 5)  grant(user, "COMMENT_5");
        if (totalComments >= 20) grant(user, "COMMENT_20");
    }

    // ── Night Owl: watched after midnight ───────────────────────────────────
    private void checkNightOwl(User user) {
        boolean nightWatch = watchHistoryRepository
                .findByUser_UserIdOrderByWatchedAtDesc(user.getUserId())
                .stream()
                .anyMatch(h -> h.getWatchedAt() != null && h.getWatchedAt().getHour() >= 0
                        && h.getWatchedAt().getHour() < 5);
        if (nightWatch) grant(user, "NIGHT_OWL");
    }

    // ── Binge Watcher: 5 episodes same series same day ──────────────────────
    private void checkBingeWatcher(User user) {
        var history = watchHistoryRepository.findByUser_UserIdOrderByWatchedAtDesc(user.getUserId());

        java.util.Map<String, Long> countMap = new java.util.HashMap<>();
        for (var h : history) {
            if (h.getEpisode() != null && h.getWatchedAt() != null
                    && h.getEpisode().getSeason() != null
                    && h.getEpisode().getSeason().getTvSeries() != null) {
                String key = h.getEpisode().getSeason().getTvSeries().getId()
                        + "_" + h.getWatchedAt().toLocalDate();
                countMap.merge(key, 1L, Long::sum);
            }
        }
        if (countMap.values().stream().anyMatch(c -> c >= 5)) {
            grant(user, "BINGE_WATCHER");
        }
    }

    // ── Genre Explorer: 5 distinct genres ───────────────────────────────────
    private void checkGenreExplorer(User user) {
        var history = watchHistoryRepository.findByUser_UserIdOrderByWatchedAtDesc(user.getUserId());
        java.util.Set<Long> genreIds = new java.util.HashSet<>();

        for (var h : history) {
            if (h.getMovie() != null && h.getMovie().getGenres() != null) {
                h.getMovie().getGenres().forEach(g -> genreIds.add(g.getId()));
            }
            if (h.getEpisode() != null
                    && h.getEpisode().getSeason() != null
                    && h.getEpisode().getSeason().getTvSeries() != null
                    && h.getEpisode().getSeason().getTvSeries().getGenres() != null) {
                h.getEpisode().getSeason().getTvSeries().getGenres()
                        .forEach(g -> genreIds.add(g.getId()));
            }
        }
        if (genreIds.size() >= 5) grant(user, "GENRE_EXPLORER");
    }

    // ── Get user achievements ────────────────────────────────────────────────
    @Transactional
    public List<UserAchievement> getUserAchievements(User user) {
        checkPremiumAchievement(user);
        return userAchievementRepository.findByUserOrderByEarnedAtDesc(user);
    }

    // ── Get ALL achievements (for showing locked ones) ───────────────────────
    @Transactional(readOnly = true)
    public List<Achievement> getAllAchievements() {
        return achievementRepository.findAll();
    }

    @Transactional
    public void checkWishlistAchievements(User user) {
        long totalWishlist = wishlistRepository.countByUser(user);

        if (totalWishlist >= 10) {
            grant(user, "WISHLIST_10");
        }
    }

    @Transactional
    public void checkPremiumAchievement(User user) {
        boolean isPremium = subscriptionRepository
                .existsByUserAndStatus(user, SubscriptionStatus.ACTIVE);

        if (isPremium) {
            grant(user, "PREMIUM_MEMBER");
        }
    }
}