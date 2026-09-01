package com.zipdaproperty.domain.region.repository;

import com.zipdaproperty.domain.region.entity.Region;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RegionRepository extends JpaRepository<Region, Long> {
}
