package com.zipdaproperty.domain.property.model;

import com.zipdaproperty.domain.property.constant.PropertyMapSort;
import com.zipdaproperty.domain.property.constant.PropertyType;
import com.zipdaproperty.domain.property.constant.PublisherType;
import com.zipdaproperty.domain.property.constant.TransactionType;
import com.zipdaproperty.global.error.custom.BusinessException;
import com.zipdaproperty.global.response.constant.CustomResponseCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

public record PropertyMapSearchCondition(

        Set<PropertyType> propertyTypes,
        Set<TransactionType> transactionTypes,
        Long minSalePrice,
        Long maxSalePrice,
        Long minDeposit,
        Long maxDeposit,
        Long minMonthlyRent,
        Long maxMonthlyRent,
        Long minMaintenanceFee,
        Long maxMaintenanceFee,
        BigDecimal minExclusiveArea,
        BigDecimal maxExclusiveArea,
        Integer roomCountMin,
        Integer roomCountMax,
        Set<PublisherType> publisherTypes,
        LocalDate approvalDateFrom,
        LocalDate approvalDateTo,
        Boolean isParkingAvailable,
        Boolean hasElevator,
        Boolean isPetAllowed,
        PropertyMapSort sort

) {

    public PropertyMapSearchCondition {
        propertyTypes = normalizeSet(
                propertyTypes,
                "매물 유형"
        );
        transactionTypes = normalizeSet(
                transactionTypes,
                "거래 유형"
        );
        publisherTypes = normalizeSet(
                publisherTypes,
                "등록 주체"
        );
        sort = sort == null
                ? PropertyMapSort.LATEST
                : sort;

        validateNonNegative(minSalePrice, "최소 매매가");
        validateNonNegative(maxSalePrice, "최대 매매가");
        validateNonNegative(minDeposit, "최소 보증금");
        validateNonNegative(maxDeposit, "최대 보증금");
        validateNonNegative(minMonthlyRent, "최소 월세");
        validateNonNegative(maxMonthlyRent, "최대 월세");
        validateNonNegative(minMaintenanceFee, "최소 관리비");
        validateNonNegative(maxMaintenanceFee, "최대 관리비");
        validatePositive(minExclusiveArea, "최소 전용면적");
        validatePositive(maxExclusiveArea, "최대 전용면적");
        validatePositive(roomCountMin, "최소 방 개수");
        validatePositive(roomCountMax, "최대 방 개수");

        validateRange(minSalePrice, maxSalePrice, "매매가");
        validateRange(minDeposit, maxDeposit, "보증금");
        validateRange(minMonthlyRent, maxMonthlyRent, "월세");
        validateRange(minMaintenanceFee, maxMaintenanceFee, "관리비");
        validateRange(minExclusiveArea, maxExclusiveArea, "전용면적");
        validateRange(roomCountMin, roomCountMax, "방 개수");

        if (approvalDateFrom != null
                && approvalDateTo != null
                && approvalDateFrom.isAfter(approvalDateTo)) {
            throw invalidRequest(
                    "사용승인일 시작일은 종료일보다 늦을 수 없습니다."
            );
        }
    }

    private static <T> Set<T> normalizeSet(
            Set<T> values,
            String fieldName
    ) {
        if (values == null || values.isEmpty()) {
            return Set.of();
        }

        if (values.stream().anyMatch(value -> value == null)) {
            throw invalidRequest(
                    fieldName + "에는 null을 포함할 수 없습니다."
            );
        }

        return Set.copyOf(values);
    }

    private static void validateNonNegative(
            Long value,
            String fieldName
    ) {
        if (value != null && value < 0L) {
            throw invalidRequest(
                    fieldName + "는 0 이상이어야 합니다."
            );
        }
    }

    private static void validatePositive(
            BigDecimal value,
            String fieldName
    ) {
        if (value != null
                && value.compareTo(BigDecimal.ZERO) <= 0) {
            throw invalidRequest(
                    fieldName + "은 0보다 커야 합니다."
            );
        }
    }

    private static void validatePositive(
            Integer value,
            String fieldName
    ) {
        if (value != null && value < 1) {
            throw invalidRequest(
                    fieldName + "는 1 이상이어야 합니다."
            );
        }
    }

    private static <T extends Comparable<? super T>>
    void validateRange(
            T min,
            T max,
            String fieldName
    ) {
        if (min != null
                && max != null
                && min.compareTo(max) > 0) {
            throw invalidRequest(
                    fieldName
                            + " 최솟값은 최댓값보다 클 수 없습니다."
            );
        }
    }

    private static BusinessException invalidRequest(
            String message
    ) {
        return new BusinessException(
                CustomResponseCode.INVALID_REQUEST,
                message
        );
    }
}
