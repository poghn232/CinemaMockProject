package com.example.superapp.entity;

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
    private int pId;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "profile_name")
    private String pName;

    @OneToMany(mappedBy = "profile")
    private List<Review> reviews = new ArrayList<>();

    @Column(name = "comment_disabled", nullable = false)
    @Builder.Default
    private Boolean commentDisabled = false;

    @Override
    public String toString() {
        return "Profile: " + pName;
    }
}
