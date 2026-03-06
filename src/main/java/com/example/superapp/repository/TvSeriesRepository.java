package com.example.superapp.repository;

import com.example.superapp.entity.TvSeries;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TvSeriesRepository extends JpaRepository<TvSeries, Long> {

    List<TvSeries> findByActiveTrueAndPublishedTrueAndNameContainingIgnoreCase(String name);

    List<TvSeries> findByActiveTrueAndPublishedTrue();
}

