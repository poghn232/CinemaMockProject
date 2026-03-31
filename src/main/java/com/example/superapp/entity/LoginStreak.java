package com.example.superapp.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "login_streaks")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class LoginStreak {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false)
    private Integer currentStreak = 0;   // streak hiện tại

    @Column(nullable = false)
    private Integer longestStreak = 0;   // streak dài nhất từ trước đến nay

    @Column(nullable = false)
    private Integer totalLoginDays = 0;  // tổng số ngày đã đăng nhập

    private LocalDate lastLoginDate;     // ngày đăng nhập gần nhất
}