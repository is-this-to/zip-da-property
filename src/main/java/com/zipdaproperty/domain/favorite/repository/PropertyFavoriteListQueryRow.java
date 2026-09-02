package com.zipdaproperty.domain.favorite.repository;

import com.zipdaproperty.domain.property.constant.PropertyType;
import com.zipdaproperty.domain.property.constant.PublisherType;
import com.zipdaproperty.domain.property.constant.TransactionType;

import java.math.BigDecimal;

public record PropertyFavoriteListQueryRow(
        Long propertyId,
        PropertyType propertyType,
        TransactionType transactionType,
        Long salePrice,
        Long deposit,
        Long monthlyRent,
        BigDecimal exclusiveArea,
        Integer floor,
        PublisherType publisherType,
        Long favoriteCount
) {
}