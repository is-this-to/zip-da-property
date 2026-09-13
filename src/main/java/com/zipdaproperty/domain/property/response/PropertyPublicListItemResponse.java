package com.zipdaproperty.domain.property.response;

import com.zipdaproperty.domain.property.constant.PropertyType;
import com.zipdaproperty.domain.property.constant.PublisherType;
import com.zipdaproperty.domain.property.constant.TransactionType;
import com.zipdaproperty.global.id.TsidString;

import java.math.BigDecimal;

public record PropertyPublicListItemResponse(
        @TsidString Long propertyId,
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
        double latitude,
        double longitude
) {
}
