package com.example.superapp.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "movie_region_blocks",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"movie_id", "region_code"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MovieRegionBlock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "region_code", nullable = false, length = 20)
    private String regionCode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "movie_id", nullable = false)
    private Movie movie;
}