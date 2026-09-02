package com.zipdaproperty.domain.property.command;

import com.zipdaproperty.domain.property.constant.PropertyType;
import com.zipdaproperty.domain.property.constant.PublisherType;
import com.zipdaproperty.domain.property.constant.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PropertyCreateCommand(
        Long regionId,
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
        String description
) {
}