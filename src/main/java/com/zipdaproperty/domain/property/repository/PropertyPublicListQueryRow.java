package com.zipdaproperty.domain.property.repository;

import com.zipdaproperty.domain.property.constant.PropertyType;
import com.zipdaproperty.domain.property.constant.PublisherType;
import com.zipdaproperty.domain.property.constant.TransactionType;
import org.locationtech.jts.geom.Point;

import java.math.BigDecimal;
import java.time.Instant;

public record PropertyPublicListQueryRow(
        Long propertyId,
        Long regionId,
        String title,
        PropertyType propertyType,
        TransactionType transactionType,
        Long salePrice,
        Long deposit,
        Long monthlyRent,
        Long maintenanceFee,
        BigDecimal exclusiveArea,
        Integer roomCount,
        PublisherType publisherType,
        String publicAddress,
        Point publicLocation,
        Instant createdAt,
        Long representativePrice
) {
}
