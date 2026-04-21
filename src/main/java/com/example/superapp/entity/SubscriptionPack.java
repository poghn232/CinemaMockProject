package com.example.superapp.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

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

    @Column(nullable = false)
    private String packPrice;

    private Integer durationDays; // ví dụ 30, 90, 365

    @Column(name = "max_profiles")
    private Integer maxProfiles; // số profile tối đa cho gói này
    @OneToMany(mappedBy = "pack")
    private List<Subscription> subscriptions;

    @Override
    public String toString() {
        return "Subscription pack " + packName;
    }
}