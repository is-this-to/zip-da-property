package com.zipdaproperty.domain.region.repository;

import com.zipdaproperty.domain.region.entity.RegionBoundary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RegionBoundaryRepository extends JpaRepository<RegionBoundary, Long> {
    Optional<RegionBoundary>
    findByRegionIdAndSimplificationLevelAndDeletedAtIsNull(
        Long regionId,
        Integer simplificationLevel
    );
}
