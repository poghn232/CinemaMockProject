package com.example.superapp.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Profile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "profile_id")
    private long profileId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "profile_name")
    private String profileName;

    @OneToMany(mappedBy = "profile")
    @Builder.Default
    private List<Review> reviews = new ArrayList<>();

    @Column(name = "comment_disabled", nullable = false)
    @Builder.Default
    private Boolean commentDisabled = false;

    @OneToMany(mappedBy = "profile")
    @Builder.Default
    private List<WatchHistory> watchHistories = new ArrayList<>();

    @Column(name = "enabled")
    @Builder.Default
    private boolean enabled = true;

    @Override
    public String toString() {
        return "Profile: " + profileName;
    }

    public Profile(String profileName) {
        this.profileName = profileName;
    }
}
