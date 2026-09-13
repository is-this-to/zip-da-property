package com.zipdaproperty.domain.property.model;

import com.zipdaproperty.global.error.custom.BusinessException;
import com.zipdaproperty.global.response.constant.CustomResponseCode;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.PrecisionModel;

public record PropertyListBounds(
        double minLat,
        double maxLat,
        double minLng,
        double maxLng
) {

    private static final int WGS84_SRID = 4326;
    private static final GeometryFactory GEOMETRY_FACTORY =
            new GeometryFactory(new PrecisionModel(), WGS84_SRID);

    public PropertyListBounds {
        validateFinite(minLat, "minLat");
        validateFinite(maxLat, "maxLat");
        validateFinite(minLng, "minLng");
        validateFinite(maxLng, "maxLng");

        if (minLat < -90.0 || minLat > 90.0
                || maxLat < -90.0 || maxLat > 90.0) {
            throw invalidRequest("위도는 -90 이상 90 이하여야 합니다.");
        }
        if (minLng < -180.0 || minLng > 180.0
                || maxLng < -180.0 || maxLng > 180.0) {
            throw invalidRequest("경도는 -180 이상 180 이하여야 합니다.");
        }
        if (minLat >= maxLat) {
            throw invalidRequest("minLat는 maxLat보다 작아야 합니다.");
        }
        if (minLng >= maxLng) {
            throw invalidRequest("minLng는 maxLng보다 작아야 합니다.");
        }
    }

    public Polygon toPolygon() {
        Polygon polygon = GEOMETRY_FACTORY.createPolygon(new Coordinate[]{
                new Coordinate(minLng, minLat),
                new Coordinate(maxLng, minLat),
                new Coordinate(maxLng, maxLat),
                new Coordinate(minLng, maxLat),
                new Coordinate(minLng, minLat)
        });
        polygon.setSRID(WGS84_SRID);
        return polygon;
    }

    private static void validateFinite(double value, String fieldName) {
        if (!Double.isFinite(value)) {
            throw invalidRequest(fieldName + "에는 유효한 숫자가 필요합니다.");
        }
    }

    private static BusinessException invalidRequest(String message) {
        return new BusinessException(CustomResponseCode.INVALID_REQUEST, message);
    }
}
