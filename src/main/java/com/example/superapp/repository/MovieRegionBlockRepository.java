package com.example.superapp.repository;

import com.example.superapp.entity.MovieRegionBlock;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface MovieRegionBlockRepository extends JpaRepository<MovieRegionBlock, Long> {

	List<MovieRegionBlock> findByMovie_Id(Long movieId);

	Optional<MovieRegionBlock> findByMovie_IdAndRegionCode(Long movieId, String regionCode);

	void deleteByMovie_IdAndRegionCode(Long movieId, String regionCode);
}