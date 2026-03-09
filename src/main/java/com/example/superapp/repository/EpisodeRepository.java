package com.example.superapp.repository;

import com.example.superapp.entity.Episode;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EpisodeRepository extends JpaRepository<Episode, Long> {
	List<Episode> findBySeasonId(Long seasonId);

}
