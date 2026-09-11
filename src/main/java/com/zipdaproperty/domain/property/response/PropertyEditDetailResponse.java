package com.zipdaproperty.domain.property.response;

import com.zipdaproperty.domain.property.constant.PropertyType;
import com.zipdaproperty.domain.property.constant.PublicationStatus;
import com.zipdaproperty.domain.property.constant.PublisherType;
import com.zipdaproperty.domain.property.constant.TransactionStatus;
import com.zipdaproperty.domain.property.constant.TransactionType;
import com.zipdaproperty.domain.property.constant.VerificationStatus;
import com.zipdaproperty.domain.property.entity.Property;
import com.zipdaproperty.global.id.TsidString;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record PropertyEditDetailResponse(

        @TsidString
        Long propertyId,

        Long version,

        @TsidString
        Long regionId,

        @TsidString
        Long apartmentComplexId,

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

        PublicationStatus publicationStatus,

        TransactionStatus transactionStatus,

        VerificationStatus verificationStatus,

        List<PropertyEditImageResponse> images

) {

    public static PropertyEditDetailResponse from(
            Property property,
            List<PropertyEditImageResponse> images
    ) {
        return new PropertyEditDetailResponse(
                property.getPropertyId(),
                property.getVersion(),
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
                property.getPublicationStatus(),
                property.getTransactionStatus(),
                property.getVerificationStatus(),
                List.copyOf(images)
        );
    }
}
