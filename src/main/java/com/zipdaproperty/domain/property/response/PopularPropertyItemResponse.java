package com.zipdaproperty.domain.property.response;

import com.zipdaproperty.domain.property.constant.PropertyType;
import com.zipdaproperty.domain.property.constant.TransactionType;
import com.zipdaproperty.global.id.TsidString;

import java.math.BigDecimal;

public record PopularPropertyItemResponse(
        @TsidString Long propertyId,
        String title,
        PropertyType propertyType,
        TransactionType transactionType,
        Long salePrice,
        Long deposit,
        Long monthlyRent,
        BigDecimal exclusiveArea,
        String locationSummary,
        String representativeImageUrl,
        long favoriteCount
) {
}
