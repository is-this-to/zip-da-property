package com.zipdaproperty.domain.property.repository;

import com.zipdaproperty.domain.region.constant.RegionType;

public record PropertyMapRegionAggregateQueryRow(
        Long regionId,
        String regionName,
        Integer regionLevel,
        RegionType regionType,
        double latitude,
        double longitude,
        long propertyCount
) {
}
