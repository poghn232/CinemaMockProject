package com.example.superapp.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tv_credits")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TvCredit {

    @EmbeddedId
    private TvCreditId id = new TvCreditId();

    @ManyToOne
    @MapsId("tvId")
    @JoinColumn(name = "tv_id")
    private TvSeries tvSeries;

    @ManyToOne
    @MapsId("personId")
    @JoinColumn(name = "person_id")
    private Person person;


    @Column(name = "character_name")
    private String character;

    private String job;
    private String department;

    private Integer creditOrder;
}
