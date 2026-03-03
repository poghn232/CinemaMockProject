package com.example.superapp.repository;

import com.example.superapp.entity.Movie;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MovieRepository extends JpaRepository<Movie, Long> {

    List<Movie> findByActiveTrueAndPublishedTrueAndTitleContainingIgnoreCase(String title);

    List<Movie> findByActiveTrueAndPublishedTrue();
}

