package com.zipdaproperty.domain.property.repository;

import com.zipdaproperty.domain.property.constant.PropertyType;
import com.zipdaproperty.domain.property.constant.TransactionType;
import org.locationtech.jts.geom.Point;

public record PropertyMapBoundsQueryRow(
        Long propertyId,
        Long regionId,
        String title,
        PropertyType propertyType,
        TransactionType transactionType,
        Long salePrice,
        Long deposit,
        Long monthlyRent,
        String publicAddress,
        Point publicLocation
) {
}
