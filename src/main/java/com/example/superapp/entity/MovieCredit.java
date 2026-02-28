package com.example.superapp.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "movie_credits")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MovieCredit {

    @EmbeddedId
    private MovieCreditId id = new MovieCreditId();

    @ManyToOne
    @MapsId("movieId")
    @JoinColumn(name = "movie_id")
    private Movie movie;

    @ManyToOne
    @MapsId("personId")
    @JoinColumn(name = "person_id")
    private Person person;

    @Column(name = "character_name")
    private String character;
    // nếu là actor
    private String job;           // Director, Producer...
    private String department;    // Acting, Directing...

    private Integer creditOrder;
}
