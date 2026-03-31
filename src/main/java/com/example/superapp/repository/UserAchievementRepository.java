package com.example.superapp.repository;

import com.example.superapp.entity.User;
import com.example.superapp.entity.UserAchievement;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface UserAchievementRepository extends JpaRepository<UserAchievement, Long> {
    List<UserAchievement> findByUserOrderByEarnedAtDesc(User user);
    boolean existsByUserAndAchievement_Code(User user, String code);
}