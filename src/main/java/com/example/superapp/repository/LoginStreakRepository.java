package com.example.superapp.repository;

import com.example.superapp.entity.LoginStreak;
import com.example.superapp.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface LoginStreakRepository extends JpaRepository<LoginStreak, Long> {
    Optional<LoginStreak> findByUser(User user);
}