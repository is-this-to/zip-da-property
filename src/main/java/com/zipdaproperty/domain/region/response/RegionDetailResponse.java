package com.zipdaproperty.domain.region.response;

import com.zipdaproperty.domain.region.constant.RegionType;

import java.math.BigDecimal;

public record RegionDetailResponse(
        Long regionId,
        String regionCode,
        String regionName,
        Integer regionLevel,
        RegionType regionType,
        CenterResponse center,
        Integer simplificationLevel,
        GeometryResponse geometry,
        BoundsResponse bounds
) {
    public record CenterResponse(
        Double latitude,
        Double longitude
    ){}

    public record GeometryResponse(
        String type,
        Object coordinates
    ){}

    public record BoundsResponse(
        CoordinateResponse southWest,
        CoordinateResponse northEast
    ){}

    public record CoordinateResponse(
        BigDecimal latitude,
        BigDecimal longitude
    ){}
}
