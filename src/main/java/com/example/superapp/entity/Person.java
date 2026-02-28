package com.example.superapp.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "persons")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Person {

    @Id
    private Long id; // TMDB person id

    @Column(nullable = false)
    private String name;

    private String profilePath;

    @Column(length = 8000)
    private String biography;

    private LocalDate birthday;
    private String placeOfBirth;

    @OneToMany(mappedBy = "person")
    private Set<MovieCredit> movieCredits = new HashSet<>();

    @OneToMany(mappedBy = "person")
    private Set<TvCredit> tvCredits = new HashSet<>();
}