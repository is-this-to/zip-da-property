package com.zipdaproperty.domain.property.policy;

import com.zipdaproperty.domain.property.constant.PropertyMapResponseType;
import org.springframework.stereotype.Component;

@Component
public class PropertyMapZoomPolicy {

    private static final int SIDO_MINIMUM_ZOOM_LEVEL = 13;
    private static final int SIGUNGU_MINIMUM_ZOOM_LEVEL = 11;
    private static final int REGION_AGGREGATE_MINIMUM_ZOOM_LEVEL = 8;
    private static final int PROPERTY_POINTS_MINIMUM_ZOOM_LEVEL = 5;

    private static final int SIDO_REGION_LEVEL = 1;
    private static final int SIGUNGU_REGION_LEVEL = 2;
    private static final int EUPMYEONDONG_REGION_LEVEL = 3;

    public PropertyMapResponseType resolveResponseType(int zoomLevel) {
        if (zoomLevel >= REGION_AGGREGATE_MINIMUM_ZOOM_LEVEL) {
            return PropertyMapResponseType.REGION_AGGREGATE;
        }

        if (zoomLevel >= PROPERTY_POINTS_MINIMUM_ZOOM_LEVEL) {
            return PropertyMapResponseType.PROPERTY_POINTS;
        }

        return PropertyMapResponseType.PROPERTY_MARKER;
    }

    public int resolveAggregationRegionLevel(int zoomLevel) {
        if (zoomLevel >= SIDO_MINIMUM_ZOOM_LEVEL) {
            return SIDO_REGION_LEVEL;
        }

        if (zoomLevel >= SIGUNGU_MINIMUM_ZOOM_LEVEL) {
            return SIGUNGU_REGION_LEVEL;
        }

        if (zoomLevel >= REGION_AGGREGATE_MINIMUM_ZOOM_LEVEL) {
            return EUPMYEONDONG_REGION_LEVEL;
        }

        throw new IllegalArgumentException(
                "지역 집계 단계는 zoomLevel 8 이상에서만 결정할 수 있습니다."
        );
    }
}
