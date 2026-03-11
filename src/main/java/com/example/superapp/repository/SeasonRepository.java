package com.example.superapp.repository;

import com.example.superapp.entity.Season;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SeasonRepository extends JpaRepository<Season, Long> {

    List<Season> findByTvSeriesId(Long tvId);

}
