package com.zipdaproperty.domain.property.model;

import org.locationtech.jts.geom.Point;

import java.time.Instant;
import java.util.Objects;

public final class PreparedPropertyAddress {

    private final Long regionId;
    private final String legalDongCode;
    private final String roadAddress;
    private final String jibunAddress;
    private final Point exactLocation;
    private final String publicAddress;
    private final Point publicLocation;
    private final Instant locationVerifiedAt;

    public PreparedPropertyAddress(
            Long regionId,
            String legalDongCode,
            String roadAddress,
            String jibunAddress,
            Point exactLocation,
            String publicAddress,
            Point publicLocation,
            Instant locationVerifiedAt
    ) {
        this.regionId = Objects.requireNonNull(regionId);
        this.legalDongCode = Objects.requireNonNull(legalDongCode);
        this.roadAddress = roadAddress;
        this.jibunAddress = jibunAddress;
        this.exactLocation = copyPoint(exactLocation);
        this.publicAddress = Objects.requireNonNull(publicAddress);
        this.publicLocation = copyPoint(publicLocation);
        this.locationVerifiedAt = Objects.requireNonNull(locationVerifiedAt);
    }

    public Long regionId() {
        return regionId;
    }

    public String legalDongCode() {
        return legalDongCode;
    }

    public String roadAddress() {
        return roadAddress;
    }

    public String jibunAddress() {
        return jibunAddress;
    }

    public Point exactLocation() {
        return copyPoint(exactLocation);
    }

    public String publicAddress() {
        return publicAddress;
    }

    public Point publicLocation() {
        return copyPoint(publicLocation);
    }

    public Instant locationVerifiedAt() {
        return locationVerifiedAt;
    }

    private static Point copyPoint(Point point) {
        return (Point) Objects.requireNonNull(point).copy();
    }
}
