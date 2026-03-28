package com.example.superapp.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "ads")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    // PRE_ROLL
    private String adType;

    // ACTIVE / INACTIVE
    private String status;

    // master.m3u8 URL
    private String srcFilm;

    private Boolean skippable;

    private Integer skipAfterSeconds;
}