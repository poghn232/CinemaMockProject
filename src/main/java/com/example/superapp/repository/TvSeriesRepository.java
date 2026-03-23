package com.example.superapp.repository;

import com.example.superapp.entity.TvSeries;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface TvSeriesRepository extends JpaRepository<TvSeries, Long> {

    List<TvSeries> findByActiveTrueAndPublishedTrueAndNameContainingIgnoreCase(String name);

    List<TvSeries> findByActiveTrueAndPublishedTrue();

    @Query("SELECT DISTINCT tv FROM TvSeries tv " +
            "LEFT JOIN FETCH tv.credits c " +
            "LEFT JOIN FETCH c.person " +
            "LEFT JOIN FETCH tv.genres " +
            "WHERE tv.active = true AND tv.published = true")
    List<TvSeries> findAllPublishedWithDetails();
}

