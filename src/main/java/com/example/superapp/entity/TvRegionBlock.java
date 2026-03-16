package com.example.superapp.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "tv_region_blocks",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"tv_id", "region_code"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TvRegionBlock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "region_code", nullable = false, length = 20)
    private String regionCode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tv_id", nullable = false)
    private TvSeries tvSeries;
}