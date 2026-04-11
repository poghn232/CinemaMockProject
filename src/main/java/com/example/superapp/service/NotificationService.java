package com.example.superapp.service;

import com.example.superapp.dto.NotificationDto;
import com.example.superapp.entity.Notification;
import com.example.superapp.entity.Profile;
import com.example.superapp.entity.User;
import com.example.superapp.repository.NotificationRepository;
import com.example.superapp.repository.ProfileRepository;
import com.example.superapp.repository.UserRepository;
import com.example.superapp.repository.WishlistRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final String TMDB_IMG = "https://image.tmdb.org/t/p/w200";

    private final NotificationRepository notificationRepository;
    private final WishlistRepository wishlistRepository;
    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // ── PUBLIC API ────────────────────────────────────────────────────────

    /** Backward compatible — không có episodeId */
    @Transactional
    public void notifyWishlistUsers(Long contentId, String contentType,
                                    String contentTitle, String posterPath,
                                    String eventType) {
        notifyWishlistUsers(contentId, contentType, contentTitle, posterPath, eventType, null, null);
    }

    /** Backward compatible — có episodeId nhưng không có episodeName */
    @Transactional
    public void notifyWishlistUsers(Long contentId, String contentType,
                                    String contentTitle, String posterPath,
                                    String eventType, Long episodeId) {
        notifyWishlistUsers(contentId, contentType, contentTitle, posterPath, eventType, episodeId, null);
    }

    /** Main method — đầy đủ params */
    @Transactional
    public void notifyWishlistUsers(Long contentId, String contentType,
                                    String contentTitle, String posterPath,
                                    String eventType, Long episodeId, String episodeName) {

        log.info("[Notification] called: contentId={}, episodeId={}, eventType={}",
                contentId, episodeId, eventType);

        String poster = posterPath != null ? TMDB_IMG + posterPath : null;

        // Now we notify profiles instead of users
        List<Profile> profiles = wishlistRepository.findProfilesByContentIdAndContentType(contentId, contentType);

        if (profiles.isEmpty()) {
            log.info("[Notification] No wishlist profiles → skip");
            return;
        }

        for (Profile profile : profiles) {
            boolean alreadyExists = notificationRepository
                    .existsByProfileAndContentIdAndContentTypeAndEventTypeAndEpisodeId(
                            profile, contentId, contentType, eventType, episodeId);

            if (alreadyExists) continue;

            Notification notif = Notification.builder()
                    .profile(profile)
                    .contentId(contentId)
                    .contentType(contentType)
                    .contentTitle(contentTitle)
                    .posterUrl(poster)
                    .eventType(eventType)
                    .episodeId(episodeId)
                    .messageKey(buildMessageKey(eventType, episodeId))
                    .messageParams(buildParams(contentTitle, episodeName))
                    .isRead(false)
                    .build();

            notificationRepository.save(notif);
        }
    }

    // ── PROFILE-FACING ───────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<NotificationDto> getNotifications(Long profileId) {
        Profile profile = getProfile(profileId);
        return notificationRepository
                .findTop20ByProfileOrderByCreatedAtDesc(profile)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public long countUnread(Long profileId) {
        return notificationRepository.countByProfileAndIsReadFalse(getProfile(profileId));
    }

    @Transactional
    public void markAllRead(Long profileId) {
        notificationRepository.markAllAsRead(getProfile(profileId));
    }

    @Transactional
    public void markOneRead(Long profileId, Long notifId) {
        notificationRepository.markOneAsRead(notifId, getProfile(profileId));
    }

    // ── HELPERS ───────────────────────────────────────────────────────────

    private Profile getProfile(Long profileId) {
        return profileRepository.findByProfileId(profileId)
                .orElseThrow(() -> new RuntimeException("Profile not found: " + profileId));
    }

    private String buildMessageKey(String eventType, Long episodeId) {
        return switch (eventType) {
            case "NEW_MOVIE"   -> "notif.new_movie";
            case "NEW_TRAILER" -> episodeId != null ? "notif.new_trailer_episode" : "notif.new_trailer";
            case "NEW_SOURCE"  -> episodeId != null ? "notif.new_source_episode" : "notif.new_source";
            case "UNPUBLISHED" -> episodeId != null ? "notif.unpublished_episode" : "notif.unpublished";
            case "ACHIEVEMENT_UNLOCK" -> "notif.achievement_unlock";
            default            -> "notif.default";
        };
    }

    private String buildParams(String title, String episodeName) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                    "title", title,
                    "episode", episodeName != null ? episodeName : ""
            ));
        } catch (Exception e) {
            return "{}";
        }
    }

    private NotificationDto toDto(Notification n) {

        String key = n.getMessageKey();

        if (key == null || key.isBlank()) {
            if ("ACHIEVEMENT_UNLOCK".equals(n.getEventType())) {
                key = "notif.achievement_unlock";
            } else {
                key = "notif.default";
            }
        }

        return new NotificationDto(
                n.getId(),
                key,
                safe(n.getMessageParams(), "{}"),
                n.getCreatedAt(),
                n.isRead(),
                n.getContentId(),
                n.getContentType(),
                n.getContentTitle(),
                n.getPosterUrl(),
                n.getEventType(),
                n.getEpisodeId(),
                n.getEventType().equals("ACHIEVEMENT_UNLOCK") ? n.getIconUrl() : null
        );
    }
    private String safe(String s, String fallback) {
        return (s == null || s.isBlank()) ? fallback : s;
    }
}