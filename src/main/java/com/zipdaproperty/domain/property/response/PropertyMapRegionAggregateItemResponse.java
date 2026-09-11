package com.zipdaproperty.domain.property.response;

import com.zipdaproperty.domain.region.constant.RegionType;
import io.swagger.v3.oas.annotations.media.Schema;

public record PropertyMapRegionAggregateItemResponse(

        @Schema(description = "집계 대상 Region ID", example = "11680")
        Long regionId,

        @Schema(description = "지역명", example = "강남구")
        String regionName,

        @Schema(
                description = "지역 단계. 1=시·도, 2=시·군·구, 3=읍·면·동",
                example = "2"
        )
        Integer regionLevel,

        @Schema(description = "지역 유형", example = "SIGUNGU")
        RegionType regionType,

        @Schema(description = "지역 집계 마커의 표시 위도", example = "37.5172")
        double latitude,

        @Schema(description = "지역 집계 마커의 표시 경도", example = "127.0473")
        double longitude,

        @Schema(description = "해당 지역에 포함된 공개 매물 수", example = "128")
        long propertyCount

) implements PropertyMapItemResponse {
}
