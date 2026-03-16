package com.example.superapp.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = true)  // null for Google-login users
    private String password;

    @Column(nullable = true, unique = true)
    private String googleId;  // set only for Google-login users

    @Column(nullable = false)
    private String role;

    @OneToMany(mappedBy = "user")
    private List<Subscription> subscriptions = new java.util.ArrayList<>();

    @OneToMany(mappedBy = "user")
    private List<Review> reviews = new java.util.ArrayList<>();

    @Column(nullable = false)
    private Boolean enabled = true;

}
