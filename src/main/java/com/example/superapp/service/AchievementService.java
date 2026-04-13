package com.example.superapp.service;

import com.example.superapp.entity.*;
import com.example.superapp.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;

import java.util.*;

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
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean grant(Profile profile, String code) {
        if (userAchievementRepository.existsByProfileAndAchievement_Code(profile, code)) {
            return false;
        }
        Achievement achievement = achievementRepository.findByCode(code).orElse(null);
        if (achievement == null) return false;

        userAchievementRepository.save(UserAchievement.builder()
                .profile(profile)
                .achievement(achievement)
                .build());

        // Notification
        Notification notif = Notification.builder()
                .profile(profile)
                .message("🏆 Achievement unlocked: " + achievement.getName())
                .contentId(0L)
                .contentType("achievement")
                .contentTitle(achievement.getName())
                .messageKey("notif.achievement_unlock")
                .messageParams(buildAchievementParams(achievement.getName()))
                .iconUrl(achievement.getIcon())
                .posterUrl(null)
                .eventType("ACHIEVEMENT_UNLOCK")
                .isRead(false)
                .build();
        notificationRepository.save(notif);

        log.info("[Achievement] profile={} earned '{}' ({})",
                profile.getProfileName(), achievement.getName(), code);
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
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void checkWatchAchievements(Profile profile) {
        long totalWatched = watchHistoryRepository
                .findAllByProfile_ProfileIdOrderByWatchedAtDesc(profile.getProfileId()).size();

        if (totalWatched >= 1)   grant(profile, "FIRST_WATCH");
        if (totalWatched >= 10)  grant(profile, "WATCH_10");
        if (totalWatched >= 50)  grant(profile, "WATCH_50");
        if (totalWatched >= 100) grant(profile, "WATCH_100");

        checkNightOwl(profile);
        checkBingeWatcher(profile);
        checkGenreExplorer(profile);
    }

    // ── Check all comment-based achievements ────────────────────────────────
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void checkCommentAchievements(Profile profile) {
        long totalComments = reviewRepository.findByProfile_ProfileId(profile.getProfileId()).size();

        if (totalComments >= 1)  grant(profile, "FIRST_COMMENT");
        if (totalComments >= 5)  grant(profile, "COMMENT_5");
        if (totalComments >= 20) grant(profile, "COMMENT_20");
    }

    // ── Night Owl: watched after midnight ───────────────────────────────────
    private void checkNightOwl(Profile profile) {
        List<WatchHistory> history = watchHistoryRepository
                .findAllByProfile_ProfileIdOrderByWatchedAtDesc(profile.getProfileId());
        boolean nightWatch = history.stream()
                .anyMatch(h -> h.getWatchedAt() != null && h.getWatchedAt().getHour() < 5);
        if (nightWatch) grant(profile, "NIGHT_OWL");
    }

    // ── Binge Watcher: 5 episodes same series same day ──────────────────────
    private void checkBingeWatcher(Profile profile) {
        List<WatchHistory> history = watchHistoryRepository
                .findAllByProfile_ProfileIdOrderByWatchedAtDesc(profile.getProfileId());

        Map<String, Long> countMap = new HashMap<>();
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
            grant(profile, "BINGE_WATCHER");
        }
    }

    // ── Genre Explorer: 5 distinct genres ───────────────────────────────────
    private void checkGenreExplorer(Profile profile) {
        List<WatchHistory> history = watchHistoryRepository
                .findAllByProfile_ProfileIdOrderByWatchedAtDesc(profile.getProfileId());
        Set<Long> genreIds = new HashSet<>();

        for (var h : history) {
            if (h.getMovie() != null && h.getMovie().getGenres() != null)
                h.getMovie().getGenres().forEach(g -> genreIds.add(g.getId()));
            if (h.getEpisode() != null
                    && h.getEpisode().getSeason() != null
                    && h.getEpisode().getSeason().getTvSeries() != null
                    && h.getEpisode().getSeason().getTvSeries().getGenres() != null)
                h.getEpisode().getSeason().getTvSeries().getGenres()
                        .forEach(g -> genreIds.add(g.getId()));
        }
        if (genreIds.size() >= 5) grant(profile, "GENRE_EXPLORER");
    }

    // ── Get user achievements ────────────────────────────────────────────────
    @Transactional
    public List<UserAchievement> getUserAchievements(Profile profile) {
        checkPremiumAchievement(profile);
        return userAchievementRepository.findByProfileOrderByEarnedAtDesc(profile);
    }

    // ── Get ALL achievements (for showing locked ones) ───────────────────────
    @Transactional(readOnly = true)
    public List<Achievement> getAllAchievements() {
        return achievementRepository.findAll();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void checkWishlistAchievements(Profile profile) {
        long totalWishlist = wishlistRepository.countByProfile(profile);
        if (totalWishlist >= 10) grant(profile, "WISHLIST_10");
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void checkPremiumAchievement(Profile profile) {
        boolean isPremium = subscriptionRepository
                .existsByUserAndStatus(profile.getUser(), SubscriptionStatus.ACTIVE);
        if (isPremium) grant(profile, "PREMIUM_MEMBER");
    }
}