package com.example.superapp.repository;


import com.example.superapp.entity.Profile;
import com.example.superapp.entity.UserAchievement;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface UserAchievementRepository extends JpaRepository<UserAchievement, Long> {
    List<UserAchievement> findByProfileOrderByEarnedAtDesc(Profile profile);
    boolean existsByProfileAndAchievement_Code(Profile profile, String code);
}