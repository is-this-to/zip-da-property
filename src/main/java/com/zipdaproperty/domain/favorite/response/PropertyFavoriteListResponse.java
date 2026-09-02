package com.zipdaproperty.domain.favorite.response;

import com.zipdaproperty.domain.property.constant.PropertyType;
import com.zipdaproperty.domain.property.constant.PublisherType;
import com.zipdaproperty.domain.property.constant.TransactionType;
import com.zipdaproperty.global.id.TsidString;

import java.math.BigDecimal;
import java.util.List;

public record PropertyFavoriteListResponse(
        List<Item> items,
        Integer nextCursor,
        boolean hasNext
) {
    public PropertyFavoriteListResponse {
        items = List.copyOf(items);
    }

    public record Item(
            @TsidString Long propertyId,
            String representativeImageUrl,
            PropertyType propertyType,
            TransactionType transactionType,
            Price displayPrice,
            BigDecimal exclusiveArea,
            String locationSummary,
            Integer floor,
            PublisherType publisherType,
            String apartmentComplexName,
            String nearestStationName,
            String subwayLineName,
            Integer distanceMeters,
            Integer estimatedWalkingMinutes,
            long favoriteCount,
            boolean isFavorite
    ) {
    }

    public record Price(
            Long salePrice,
            Long deposit,
            Long monthlyRent
    ) {
    }
}
