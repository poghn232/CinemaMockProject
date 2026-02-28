package com.example.superapp.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Entity
@Table(name = "subscription_packs")
@Getter
@Setter
public class SubscriptionPack {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long packId;

    @Column(nullable = false)
    private String packName;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal packPrice;

    private Integer durationDays; // ví dụ 30, 90, 365

    @OneToMany(mappedBy = "pack")
    private List<Subscription> subscriptions;
}