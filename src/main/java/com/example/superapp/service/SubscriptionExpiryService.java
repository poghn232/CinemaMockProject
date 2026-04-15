package com.example.superapp.service;

import com.example.superapp.entity.Subscription;
import com.example.superapp.entity.SubscriptionStatus;
import com.example.superapp.entity.User;
import com.example.superapp.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Lazy-check service: mỗi khi user truy cập hệ thống (login, reload, gọi API...),
 * service này sẽ kiểm tra tất cả subscription ACTIVE của user đó
 * và tự động chuyển sang EXPIRED nếu endDate đã qua.
 */
@Service
@RequiredArgsConstructor
public class SubscriptionExpiryService {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionExpiryService.class);

    private final SubscriptionRepository subscriptionRepository;

    /**
     * Kiểm tra và cập nhật tất cả subscription hết hạn của một user.
     * Được gọi mỗi khi user truy cập hệ thống.
     *
     * @param user the user to check
     * @return số lượng subscription đã được cập nhật thành EXPIRED
     */
    @Transactional
    public int expireOverdueSubscriptions(User user) {
        if (user == null || user.getSubscriptions() == null) {
            return 0;
        }

        LocalDateTime now = LocalDateTime.now();
        int expiredCount = 0;

        List<Subscription> activeSubscriptions = subscriptionRepository
                .findByUserAndStatusAndEndDateBefore(user, SubscriptionStatus.ACTIVE, now);

        for (Subscription sub : activeSubscriptions) {
            sub.setStatus(SubscriptionStatus.EXPIRED);
            subscriptionRepository.save(sub);
            expiredCount++;
            log.info("Subscription [id={}] of user [{}] has been marked as EXPIRED (endDate={})",
                    sub.getSubId(), user.getUsername(), sub.getEndDate());
        }

        return expiredCount;
    }
}
