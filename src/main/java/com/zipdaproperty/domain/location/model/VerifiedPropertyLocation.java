package com.zipdaproperty.domain.location.model;

import org.locationtech.jts.geom.Point;

import java.util.Objects;

public record VerifiedPropertyLocation(
        Long regionId,
        String legalDongCode,
        Point exactLocation
) {

    public VerifiedPropertyLocation {
        Objects.requireNonNull(
                regionId,
                "검증된 Region ID는 필수입니다."
        );
        Objects.requireNonNull(
                legalDongCode,
                "검증된 법정동 코드는 필수입니다."
        );
        Objects.requireNonNull(
                exactLocation,
                "검증된 정확 좌표는 필수입니다."
        );

        exactLocation = (Point) exactLocation.copy();
    }

    @Override
    public Point exactLocation() {
        return (Point) exactLocation.copy();
    }
}
