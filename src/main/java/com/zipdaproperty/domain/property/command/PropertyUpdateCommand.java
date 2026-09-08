package com.zipdaproperty.domain.property.command;

import com.zipdaproperty.domain.property.constant.PropertyType;
import com.zipdaproperty.domain.property.constant.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PropertyUpdateCommand(
        Long requestedVersion,  //마지막으로 조회한 매물 버전
        Long regionId,
        Long apartmentComplexId,  // 아파트 단지 ID
        PropertyType propertyType,  // 수정 후 매물 종류
        TransactionType transactionType,  // 수정 후 거래 유형
        Long salePrice,
        Long deposit,
        Long monthlyRent,
        Long maintenanceFee,
        BigDecimal supplyArea,   // 공급면적
        BigDecimal exclusiveArea,   // 전용면적
        Integer roomCount,
        Integer bathroomCount,
        Integer floor,
        Integer totalFloor,
        String floorCondition,
        String direction,    // 매물의 방향(안쓸예정?)
        LocalDate approvalDate,  // 건축물의 사용승인일 (안쓸예정?)
        String buildingUse,  // 건축물의 사용 용도 Ex) 공동주태그업무시설 등 (안쓸예정?)
        Boolean isParkingAvailable,
        Boolean hasElevator,
        Boolean isPetAllowed,
        String title,
        String description
) {
}