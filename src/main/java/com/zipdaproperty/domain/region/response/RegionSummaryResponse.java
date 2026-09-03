package com.zipdaproperty.domain.region.response;

import com.zipdaproperty.domain.region.constant.RegionType;
import com.zipdaproperty.domain.region.entity.Region;

public record RegionSummaryResponse(
        Long regionId,
        String regionCode,
        String regionName,
        Integer regionLevel,
        RegionType regionType,
        Boolean hasChildren
) {
    public static RegionSummaryResponse from(Region region){
        Boolean publicHasChildren =
                region.getRegionLevel() < 3
                    && Boolean.TRUE.equals(
                            region.getHasChildren()
                );

        return new RegionSummaryResponse(
                region.getRegionId(),
                region.getRegionCode(),
                region.getRegionName(),
                region.getRegionLevel(),
                region.getRegionType(),
                publicHasChildren
        );
    }
}
