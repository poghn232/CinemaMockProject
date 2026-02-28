package com.example.superapp.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "seasons")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Season {

    @Id
    private Long id;

    private String name;
    private Integer seasonNumber;
    private String posterPath;

    @ManyToOne
    @JoinColumn(name = "tv_id")
    private TvSeries tvSeries;

    @OneToMany(mappedBy = "season", cascade = CascadeType.ALL)
    private Set<Episode> episodes = new HashSet<>();
}
