package com.zipdaproperty.domain.property.repository;

import com.zipdaproperty.domain.property.entity.Property;
import org.locationtech.jts.geom.Point;

public record PropertyPublicDetailQueryRow(
        Property property,
        String publicAddress,
        Point publicLocation
) {
}
