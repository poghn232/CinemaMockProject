package com.example.superapp.service;

import com.example.superapp.event.ContentAddedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.annotation.Propagation;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationTriggerService {

    private final NotificationService notificationService;

    // ✅ Chạy SAU KHI transaction commit xong → data đã có trong DB

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(ContentAddedEvent event) {
        log.info("[NotificationTrigger] handle() contentId={}, episodeId={}",
                event.contentId(), event.episodeId());
        notificationService.notifyWishlistUsers(
                event.contentId(),
                event.contentType(),
                event.contentTitle(),
                event.posterPath(),
                event.eventType(),
                event.episodeId(),
                event.episodeName()
        );
    }
}