package com.example.superapp.repository;

import com.example.superapp.entity.Movie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface MovieRepository extends JpaRepository<Movie, Long> {

    List<Movie> findByActiveTrueAndPublishedTrueAndTitleContainingIgnoreCase(String title);

    List<Movie> findByActiveTrueAndPublishedTrue();

    @Query("SELECT DISTINCT m FROM Movie m " +
            "LEFT JOIN FETCH m.credits c " +
            "LEFT JOIN FETCH c.person " +
            "LEFT JOIN FETCH m.genres " +
            "WHERE m.active = true AND m.published = true")
    List<Movie> findAllPublishedWithDetails();
}

