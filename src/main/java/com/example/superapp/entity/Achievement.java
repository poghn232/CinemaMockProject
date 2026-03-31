package com.example.superapp.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "achievements")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class Achievement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code; // FIRST_WATCH, BINGE_WATCHER, ...

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false, length = 10)
    private String icon; // emoji

    @Column(nullable = false, length = 20)
    private String rarity; // COMMON, RARE, EPIC, LEGENDARY
}