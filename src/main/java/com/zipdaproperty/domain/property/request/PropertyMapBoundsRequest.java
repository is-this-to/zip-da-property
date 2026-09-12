package com.zipdaproperty.domain.property.request;

import com.zipdaproperty.domain.property.constant.PropertyMapSort;
import com.zipdaproperty.domain.property.constant.PropertyType;
import com.zipdaproperty.domain.property.constant.PublisherType;
import com.zipdaproperty.domain.property.constant.TransactionType;
import com.zipdaproperty.domain.property.model.PropertyMapBounds;
import com.zipdaproperty.domain.property.model.PropertyMapSearchCondition;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

public record PropertyMapBoundsRequest(

        @Schema(
                description = "지도 남서쪽 위도",
                example = "37.45"
        )
        @NotNull(message = "minLat은 필수입니다.")
        @DecimalMin(
                value = "-90.0",
                message = "minLat은 -90 이상이어야 합니다."
        )
        @DecimalMax(
                value = "90.0",
                message = "minLat은 90 이하여야 합니다."
        )
        Double minLat,

        @Schema(
                description = "지도 북동쪽 위도",
                example = "37.55"
        )
        @NotNull(message = "maxLat은 필수입니다.")
        @DecimalMin(
                value = "-90.0",
                message = "maxLat은 -90 이상이어야 합니다."
        )
        @DecimalMax(
                value = "90.0",
                message = "maxLat은 90 이하여야 합니다."
        )
        Double maxLat,

        @Schema(
                description = "지도 남서쪽 경도",
                example = "126.95"
        )
        @NotNull(message = "minLng는 필수입니다.")
        @DecimalMin(
                value = "-180.0",
                message = "minLng는 -180 이상이어야 합니다."
        )
        @DecimalMax(
                value = "180.0",
                message = "minLng는 180 이하여야 합니다."
        )
        Double minLng,

        @Schema(
                description = "지도 북동쪽 경도",
                example = "127.10"
        )
        @NotNull(message = "maxLng는 필수입니다.")
        @DecimalMin(
                value = "-180.0",
                message = "maxLng는 -180 이상이어야 합니다."
        )
        @DecimalMax(
                value = "180.0",
                message = "maxLng는 180 이하여야 합니다."
        )
        Double maxLng,

        @Schema(
                description = "Kakao ROADMAP 확대 수준. 1은 가장 확대, 14는 가장 축소",
                example = "6"
        )
        @NotNull(message = "zoomLevel은 필수입니다.")
        @Min(
                value = 1,
                message = "zoomLevel은 1 이상이어야 합니다."
        )
        @Max(
                value = 14,
                message = "zoomLevel은 14 이하여야 합니다."
        )
        Integer zoomLevel,

        @Schema(description = "매물 유형 목록")
        @Size(max = 4, message = "매물 유형은 최대 4개까지 선택할 수 있습니다.")
        Set<@NotNull PropertyType> propertyTypes,

        @Schema(description = "거래 유형 목록")
        @Size(max = 3, message = "거래 유형은 최대 3개까지 선택할 수 있습니다.")
        Set<@NotNull TransactionType> transactionTypes,

        @Schema(description = "최소 매매가", example = "300000000")
        @PositiveOrZero(message = "최소 매매가는 0 이상이어야 합니다.")
        Long minSalePrice,

        @Schema(description = "최대 매매가", example = "900000000")
        @PositiveOrZero(message = "최대 매매가는 0 이상이어야 합니다.")
        Long maxSalePrice,

        @Schema(description = "최소 보증금", example = "10000000")
        @PositiveOrZero(message = "최소 보증금은 0 이상이어야 합니다.")
        Long minDeposit,

        @Schema(description = "최대 보증금", example = "500000000")
        @PositiveOrZero(message = "최대 보증금은 0 이상이어야 합니다.")
        Long maxDeposit,

        @Schema(description = "최소 월세", example = "300000")
        @PositiveOrZero(message = "최소 월세는 0 이상이어야 합니다.")
        Long minMonthlyRent,

        @Schema(description = "최대 월세", example = "3000000")
        @PositiveOrZero(message = "최대 월세는 0 이상이어야 합니다.")
        Long maxMonthlyRent,

        @Schema(description = "최소 관리비", example = "0")
        @PositiveOrZero(message = "최소 관리비는 0 이상이어야 합니다.")
        Long minMaintenanceFee,

        @Schema(description = "최대 관리비", example = "300000")
        @PositiveOrZero(message = "최대 관리비는 0 이상이어야 합니다.")
        Long maxMaintenanceFee,

        @Schema(description = "최소 전용면적(m²)", example = "20.0")
        @DecimalMin(
                value = "0.0",
                inclusive = false,
                message = "최소 전용면적은 0보다 커야 합니다."
        )
        BigDecimal minExclusiveArea,

        @Schema(description = "최대 전용면적(m²)", example = "120.0")
        @DecimalMin(
                value = "0.0",
                inclusive = false,
                message = "최대 전용면적은 0보다 커야 합니다."
        )
        BigDecimal maxExclusiveArea,

        @Schema(description = "최소 방 개수", example = "1")
        @Min(value = 1, message = "최소 방 개수는 1 이상이어야 합니다.")
        Integer roomCountMin,

        @Schema(description = "최대 방 개수", example = "3")
        @Min(value = 1, message = "최대 방 개수는 1 이상이어야 합니다.")
        Integer roomCountMax,

        @Schema(description = "등록 주체 목록")
        @Size(max = 3, message = "등록 주체는 최대 3개까지 선택할 수 있습니다.")
        Set<@NotNull PublisherType> publisherTypes,

        @Schema(description = "사용승인일 시작일", example = "2010-01-01")
        LocalDate approvalDateFrom,

        @Schema(description = "사용승인일 종료일", example = "2026-12-31")
        LocalDate approvalDateTo,

        @Schema(description = "주차 가능 여부", nullable = true)
        Boolean isParkingAvailable,

        @Schema(description = "엘리베이터 여부", nullable = true)
        Boolean hasElevator,

        @Schema(description = "반려동물 가능 여부", nullable = true)
        Boolean isPetAllowed,

        @Schema(
                description = "정렬 기준",
                example = "LATEST",
                defaultValue = "LATEST"
        )
        PropertyMapSort sort

) {

    public PropertyMapBounds toBounds() {
        return new PropertyMapBounds(
                minLat,
                maxLat,
                minLng,
                maxLng,
                zoomLevel
        );
    }

    public PropertyMapSearchCondition toSearchCondition() {
        return new PropertyMapSearchCondition(
                propertyTypes,
                transactionTypes,
                minSalePrice,
                maxSalePrice,
                minDeposit,
                maxDeposit,
                minMonthlyRent,
                maxMonthlyRent,
                minMaintenanceFee,
                maxMaintenanceFee,
                minExclusiveArea,
                maxExclusiveArea,
                roomCountMin,
                roomCountMax,
                publisherTypes,
                approvalDateFrom,
                approvalDateTo,
                isParkingAvailable,
                hasElevator,
                isPetAllowed,
                sort
        );
    }
}
