package com.example.superapp.repository;

import com.example.superapp.entity.TvRegionBlock;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface TvRegionBlockRepository extends JpaRepository<TvRegionBlock, Long> {

	List<TvRegionBlock> findByTvSeries_Id(Long tvId);

	Optional<TvRegionBlock> findByTvSeries_IdAndRegionCode(Long tvId, String regionCode);

	void deleteByTvSeries_IdAndRegionCode(Long tvId, String regionCode);
}
