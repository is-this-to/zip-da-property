package com.zipdaproperty.domain.location.model;

import org.locationtech.jts.geom.Point;

import java.util.Objects;

public record GeneratedPublicLocation(
        Point publicLocation
) {

    public GeneratedPublicLocation {
        Objects.requireNonNull(
                publicLocation,
                "생성된 공개 좌표는 필수입니다."
        );

        publicLocation = (Point) publicLocation.copy();
    }

    @Override
    public Point publicLocation() {
        return (Point) publicLocation.copy();
    }
}
