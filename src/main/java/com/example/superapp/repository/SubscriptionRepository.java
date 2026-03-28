package com.example.superapp.repository;

import com.example.superapp.entity.Subscription;
import com.example.superapp.entity.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    boolean existsByUser_UserIdAndStatus(Long userId, SubscriptionStatus status);
    boolean existsByUser_UserIdAndStatusAndEndDateAfter(
            Long userId,
            SubscriptionStatus status,
            LocalDateTime now
    );
}