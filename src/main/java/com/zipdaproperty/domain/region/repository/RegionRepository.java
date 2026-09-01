package com.zipdaproperty.domain.region.repository;

import com.zipdaproperty.domain.region.entity.Region;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RegionRepository extends JpaRepository<Region, Long> {
    Optional<Region> findByRegionIdAndIsActiveTrue(Long regionId);
}
