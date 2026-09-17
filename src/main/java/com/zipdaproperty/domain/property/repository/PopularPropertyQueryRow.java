package com.zipdaproperty.domain.property.repository;

import com.zipdaproperty.domain.property.constant.PropertyType;
import com.zipdaproperty.domain.property.constant.TransactionType;

import java.math.BigDecimal;

public record PopularPropertyQueryRow(
        Long propertyId,
        String title,
        PropertyType propertyType,
        TransactionType transactionType,
        Long salePrice,
        Long deposit,
        Long monthlyRent,
        BigDecimal exclusiveArea,
        String locationSummary,
        Long favoriteCount
) {
}
