package com.example.superapp.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Table(name = "profiles")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Profile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long profileId;

    @Column(name = "profile_name", nullable = false)
    private String profileName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "comment_disabled")
    private Boolean commentDisabled;

    @OneToMany(mappedBy = "profile", cascade = CascadeType.ALL)
    private List<Review> reviews;

    @OneToMany(mappedBy = "profile", cascade = CascadeType.ALL)
    private List<Notification> notifications;

    @OneToMany(mappedBy = "profile", cascade = CascadeType.ALL)
    private List<WatchHistory> watchHistories;

    @OneToMany(mappedBy = "profile", cascade = CascadeType.ALL)
    private List<Wishlist> wishlists;

    @Override
    public String toString() {
        return "Profile " + profileId + " (" + profileName + ")";
    }
}
