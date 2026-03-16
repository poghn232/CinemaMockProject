package com.example.superapp.service;

import com.example.superapp.dto.NotificationDto;
import com.example.superapp.entity.Notification;
import com.example.superapp.entity.User;
import com.example.superapp.repository.NotificationRepository;
import com.example.superapp.repository.UserRepository;
import com.example.superapp.repository.WishlistRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final String TMDB_IMG = "https://image.tmdb.org/t/p/w200";

    private final NotificationRepository notificationRepository;
    private final WishlistRepository wishlistRepository;
    private final UserRepository userRepository;

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

        String message = buildMessage(eventType, contentTitle, episodeId, episodeName);
        String poster = posterPath != null ? TMDB_IMG + posterPath : null;

        List<User> users = wishlistRepository.findUsersByContentIdAndContentType(contentId, contentType);
        log.info("[Notification] Found {} wishlist users for {} #{}", users.size(), contentType, contentId);

        if (users.isEmpty()) {
            log.info("[Notification] No wishlist users → skip");
            return;
        }

        for (User user : users) {
            boolean alreadyExists = notificationRepository
                    .existsByUserAndContentIdAndContentTypeAndEventTypeAndEpisodeId(
                            user, contentId, contentType, eventType, episodeId);
            if (alreadyExists) {
                log.info("[Notification] duplicate, skip user={}", user.getUsername());
                continue;
            }

            Notification notif = Notification.builder()
                    .user(user)
                    .message(message)
                    .contentId(contentId)
                    .contentType(contentType)
                    .contentTitle(contentTitle)
                    .posterUrl(poster)
                    .eventType(eventType)
                    .episodeId(episodeId)
                    .isRead(false)
                    .build();

            notificationRepository.save(notif);
            log.info("[Notification] SAVED for user={}", user.getUsername());
        }
    }

    // ── USER-FACING QUERIES ───────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<NotificationDto> getNotifications(String username) {
        User user = getUser(username);
        return notificationRepository
                .findTop20ByUserOrderByCreatedAtDesc(user)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public long countUnread(String username) {
        return notificationRepository.countByUserAndIsReadFalse(getUser(username));
    }

    @Transactional
    public void markAllRead(String username) {
        notificationRepository.markAllAsRead(getUser(username));
    }

    @Transactional
    public void markOneRead(String username, Long notifId) {
        notificationRepository.markOneAsRead(notifId, getUser(username));
    }

    // ── PRIVATE HELPERS ───────────────────────────────────────────────────

    private User getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
    }

    private String buildMessage(String eventType, String title, Long episodeId, String episodeName) {
        String epLabel = (episodeName != null && !episodeName.isBlank())
                ? "\"" + episodeName + "\""
                : "A new episode";

        return switch (eventType) {
            case "NEW_MOVIE"   -> "🎬 \"" + title + "\" has been added to MovieZone!";
            case "NEW_TRAILER" -> episodeId != null
                    ? "🎥 " + epLabel + " of \"" + title + "\" now has a trailer!"
                    : "🎥 Trailer for \"" + title + "\" is now available!";
            case "NEW_SOURCE"  -> episodeId != null
                    ? "▶️ " + epLabel + " of \"" + title + "\" is now available to watch!"
                    : "▶️ \"" + title + "\" is now ready to watch!";
            case "UNPUBLISHED" -> episodeId != null
                    ? "⏸️ " + epLabel + " of \"" + title + "\" has been temporarily removed."
                    : "⏸️ \"" + title + "\" has been temporarily removed.";
            default            -> "🔔 Update for \"" + title + "\"";
        };
    }

    private NotificationDto toDto(Notification n) {
        return new NotificationDto(
                n.getId(),
                n.getMessage(),
                n.getCreatedAt(),
                n.isRead(),
                n.getContentId(),
                n.getContentType(),
                n.getContentTitle(),
                n.getPosterUrl(),
                n.getEventType(),
                n.getEpisodeId()  // ✅ thêm episodeId vào DTO
        );
    }
}