package com.zipdaproperty.domain.property.response;

import com.zipdaproperty.domain.option.response.PropertyDetailOptionResponse;
import com.zipdaproperty.domain.property.constant.PropertyType;
import com.zipdaproperty.domain.property.constant.PublisherType;
import com.zipdaproperty.domain.property.constant.TransactionType;
import com.zipdaproperty.domain.property.entity.Property;
import com.zipdaproperty.global.id.TsidString;
import org.locationtech.jts.geom.Point;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record PropertyPublicDetailResponse(
        @TsidString Long propertyId,
        Long regionId,
        @TsidString Long apartmentComplexId,
        PublisherType publisherType,
        PropertyType propertyType,
        TransactionType transactionType,
        Long salePrice,
        Long deposit,
        Long monthlyRent,
        Long maintenanceFee,
        BigDecimal supplyArea,
        BigDecimal exclusiveArea,
        Integer roomCount,
        Integer bathroomCount,
        Integer floor,
        Integer totalFloor,
        String floorCondition,
        String direction,
        LocalDate approvalDate,
        String buildingUse,
        Boolean isParkingAvailable,
        Boolean hasElevator,
        Boolean isPetAllowed,
        String title,
        String description,
        String publicAddress,
        double latitude,
        double longitude,
        List<PropertyDetailOptionResponse> options,
        List<PropertyPublicDetailImageResponse> images,
        long favoriteCount,
        boolean isFavorite
) {
    public static PropertyPublicDetailResponse from(
            Property property,
            String publicAddress,
            Point publicLocation,
            List<PropertyDetailOptionResponse> options,
            List<PropertyPublicDetailImageResponse> images,
            long favoriteCount,
            boolean isFavorite
    ) {
        return new PropertyPublicDetailResponse(
                property.getPropertyId(),
                property.getRegionId(),
                property.getApartmentComplexId(),
                property.getPublisherType(),
                property.getPropertyType(),
                property.getTransactionType(),
                property.getSalePrice(),
                property.getDeposit(),
                property.getMonthlyRent(),
                property.getMaintenanceFee(),
                property.getSupplyArea(),
                property.getExclusiveArea(),
                property.getRoomCount(),
                property.getBathroomCount(),
                property.getFloor(),
                property.getTotalFloor(),
                property.getFloorCondition(),
                property.getDirection(),
                property.getApprovalDate(),
                property.getBuildingUse(),
                property.getIsParkingAvailable(),
                property.getHasElevator(),
                property.getIsPetAllowed(),
                property.getTitle(),
                property.getDescription(),
                publicAddress,
                publicLocation.getY(),
                publicLocation.getX(),
                List.copyOf(options),
                List.copyOf(images),
                favoriteCount,
                isFavorite
        );
    }
}
