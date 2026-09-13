package com.zipdaproperty.domain.property.model;

import com.zipdaproperty.global.error.custom.BusinessException;
import com.zipdaproperty.global.response.constant.CustomResponseCode;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.PrecisionModel;

public record PropertyMapBounds(
        double minLat,
        double maxLat,
        double minLng,
        double maxLng,
        int zoomLevel
) {

    private static final int WGS84_SRID = 4326;

    private static final GeometryFactory GEOMETRY_FACTORY =
            new GeometryFactory(
                    new PrecisionModel(),
                    WGS84_SRID
            );

    public PropertyMapBounds {
        validateFinite(minLat, "minLat");
        validateFinite(maxLat, "maxLat");
        validateFinite(minLng, "minLng");
        validateFinite(maxLng, "maxLng");

        validateLatitude(minLat, "minLat");
        validateLatitude(maxLat, "maxLat");
        validateLongitude(minLng, "minLng");
        validateLongitude(maxLng, "maxLng");

        if (minLat >= maxLat) {
            throw invalidRequest(
                    "minLat은 maxLat보다 작아야 합니다."
            );
        }

        if (minLng >= maxLng) {
            throw invalidRequest(
                    "minLng는 maxLng보다 작아야 합니다."
            );
        }

        if (zoomLevel < 1 || zoomLevel > 14) {
            throw invalidRequest(
                    "zoomLevel은 1 이상 14 이하여야 합니다."
            );
        }
    }

    /**
     * 현재 지도 화면을 나타내는 Polygon을 생성한다.
     * JTS Point/Coordinate는 X=경도, Y=위도 순서다.
     */
    public Polygon toPolygon() {
        Coordinate[] coordinates = {
                new Coordinate(minLng, minLat),
                new Coordinate(maxLng, minLat),
                new Coordinate(maxLng, maxLat),
                new Coordinate(minLng, maxLat),
                new Coordinate(minLng, minLat)
        };

        Polygon polygon =
                GEOMETRY_FACTORY.createPolygon(
                        coordinates
                );

        polygon.setSRID(WGS84_SRID);

        return polygon;
    }

    private static void validateFinite(
            double value,
            String fieldName
    ) {
        if (!Double.isFinite(value)) {
            throw invalidRequest(
                    fieldName + "에는 유효한 숫자가 필요합니다."
            );
        }
    }

    private static void validateLatitude(
            double latitude,
            String fieldName
    ) {
        if (latitude < -90.0 || latitude > 90.0) {
            throw invalidRequest(
                    fieldName + "은 -90 이상 90 이하여야 합니다."
            );
        }
    }

    private static void validateLongitude(
            double longitude,
            String fieldName
    ) {
        if (longitude < -180.0 || longitude > 180.0) {
            throw invalidRequest(
                    fieldName + "는 -180 이상 180 이하여야 합니다."
            );
        }
    }

    private static BusinessException invalidRequest(
            String message
    ) {
        return new BusinessException(
                CustomResponseCode.INVALID_REQUEST,
                message
        );
    }
}
