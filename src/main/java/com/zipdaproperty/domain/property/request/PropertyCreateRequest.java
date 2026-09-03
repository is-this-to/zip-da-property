package com.zipdaproperty.domain.property.request;

import com.zipdaproperty.domain.property.command.PropertyCreateCommand;
import com.zipdaproperty.domain.property.constant.PropertyType;
import com.zipdaproperty.domain.property.constant.PublisherType;
import com.zipdaproperty.domain.property.constant.TransactionType;
import com.zipdaproperty.global.id.TsidString;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PropertyCreateRequest(

        @TsidString
        @NotNull(message = "지역 ID는 필수입니다.")
        Long regionId,

        @TsidString
        Long apartmentComplexId,

        @NotNull(message = "등록 주체 유형은 필수입니다.")
        PublisherType publisherType,

        @NotNull(message = "매물 유형은 필수입니다.")
        PropertyType propertyType,

        @NotNull(message = "거래 유형은 필수입니다.")
        TransactionType transactionType,

        @PositiveOrZero(message = "매매가는 0 이상이어야 합니다.")
        Long salePrice,

        @PositiveOrZero(message = "보증금은 0 이상이어야 합니다.")
        Long deposit,

        @PositiveOrZero(message = "월세는 0 이상이어야 합니다.")
        Long monthlyRent,

        @PositiveOrZero(message = "관리비는 0 이상이어야 합니다.")
        Long maintenanceFee,

        @DecimalMin(
                value = "0.00",
                inclusive = false,
                message = "공급면적은 0보다 커야 합니다."
        )
        @Digits(
                integer = 8,
                fraction = 2,
                message = "공급면적은 정수 8자리와 소수점 이하 2자리까지 입력할 수 있습니다."
        )
        BigDecimal supplyArea,

        @NotNull(message = "전용면적은 필수입니다.")
        @DecimalMin(
                value = "0.00",
                inclusive = false,
                message = "전용면적은 0보다 커야 합니다."
        )
        @Digits(
                integer = 8,
                fraction = 2,
                message = "전용면적은 정수 8자리와 소수점 이하 2자리까지 입력할 수 있습니다."
        )
        BigDecimal exclusiveArea,

        @PositiveOrZero(message = "방 개수는 0 이상이어야 합니다.")
        Integer roomCount,

        @PositiveOrZero(message = "욕실 개수는 0 이상이어야 합니다.")
        Integer bathroomCount,

        Integer floor,

        @Positive(message = "전체 층수는 0보다 커야 합니다.")
        Integer totalFloor,

        @Size(
                max = 30,
                message = "층 조건은 30자 이하여야 합니다."
        )
        String floorCondition,

        @Size(
                max = 20,
                message = "방향은 20자 이하여야 합니다."
        )
        String direction,

        LocalDate approvalDate,

        @Size(
                max = 100,
                message = "건축물 용도는 100자 이하여야 합니다."
        )
        String buildingUse,

        Boolean isParkingAvailable,

        Boolean hasElevator,

        Boolean isPetAllowed,

        @NotBlank(message = "매물 제목은 필수입니다.")
        @Size(
                max = 200,
                message = "매물 제목은 200자 이하여야 합니다."
        )
        String title,

        @NotBlank(message = "매물 설명은 필수입니다.")
        String description

) {

    public PropertyCreateCommand toCommand() {
        return new PropertyCreateCommand(
                regionId,
                apartmentComplexId,
                publisherType,
                propertyType,
                transactionType,
                salePrice,
                deposit,
                monthlyRent,
                maintenanceFee,
                supplyArea,
                exclusiveArea,
                roomCount,
                bathroomCount,
                floor,
                totalFloor,
                floorCondition,
                direction,
                approvalDate,
                buildingUse,
                isParkingAvailable,
                hasElevator,
                isPetAllowed,
                title,
                description
        );
    }
}