package com.zipdaproperty.domain.property.service;

import com.zipdaproperty.domain.property.command.PropertyUpdateCommand;
import com.zipdaproperty.domain.property.constant.PropertyType;
import com.zipdaproperty.global.error.custom.BusinessException;
import com.zipdaproperty.global.response.constant.CustomResponseCode;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class PropertyUpdatePolicy {

    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private static final int AREA_MAX_INTEGER_DIGITS = 8;
    private static final int AREA_MAX_FRACTION_DIGITS = 2;

    private static final int FLOOR_CONDITION_MAX_LENGTH = 30;
    private static final int DIRECTION_MAX_LENGTH = 20;
    private static final int BUILDING_USE_MAX_LENGTH = 100;
    private static final int TITLE_MAX_LENGTH = 200;

    public void validate(PropertyUpdateCommand command) {
        validateRegionId(command.regionId());

        validatePropertyType(command.propertyType());

        validateApartmentComplex(
                command.propertyType(),
                command.apartmentComplexId()
        );

        validateNonNegativeLong(
                "관리비",
                command.maintenanceFee()
        );

        validateArea(
                "공급면적",
                command.supplyArea(),
                false
        );

        validateArea(
                "전용면적",
                command.exclusiveArea(),
                true
        );

        validateNonNegativeInteger(
                "방 개수",
                command.roomCount()
        );

        validateNonNegativeInteger(
                "욕실 개수",
                command.bathroomCount()
        );

        validateTotalFloor(command.totalFloor());

        validateMaxLength(
                "층 조건",
                command.floorCondition(),
                FLOOR_CONDITION_MAX_LENGTH
        );

        validateMaxLength(
                "방향",
                command.direction(),
                DIRECTION_MAX_LENGTH
        );

        validateMaxLength(
                "건축물 용도",
                command.buildingUse(),
                BUILDING_USE_MAX_LENGTH
        );

        validateRequiredText(
                "매물 제목",
                command.title()
        );

        validateMaxLength(
                "매물 제목",
                command.title(),
                TITLE_MAX_LENGTH
        );

        validateRequiredText(
                "매물 설명",
                command.description()
        );
    }

    private void validateRegionId(Long regionId) {
        if (regionId == null || regionId <= 0) {
            throw invalidRequest(
                    "지역 ID는 1 이상이어야 합니다."
            );
        }
    }

    private void validatePropertyType(PropertyType propertyType) {
        if (propertyType == null) {
            throw invalidRequest(
                    "매물 유형은 필수입니다."
            );
        }
    }

    private void validateApartmentComplex(
            PropertyType propertyType,
            Long apartmentComplexId
    ) {
        if (apartmentComplexId != null
                && apartmentComplexId <= 0) {
            throw invalidRequest(
                    "아파트 단지 ID는 1 이상이어야 합니다."
            );
        }

        if (propertyType != PropertyType.APARTMENT
                && apartmentComplexId != null) {
            throw invalidRequest(
                    "아파트가 아닌 매물에는 아파트 단지 ID를 지정할 수 없습니다."
            );
        }
    }

    private void validateNonNegativeLong(
            String fieldName,
            Long value
    ) {
        if (value != null && value < 0) {
            throw invalidRequest(
                    fieldName + "는 0 이상이어야 합니다."
            );
        }
    }

    private void validateNonNegativeInteger(
            String fieldName,
            Integer value
    ) {
        if (value != null && value < 0) {
            throw invalidRequest(
                    fieldName + "는 0 이상이어야 합니다."
            );
        }
    }

    private void validateArea(
            String fieldName,
            BigDecimal value,
            boolean required
    ) {
        if (value == null) {
            if (required) {
                throw invalidRequest(
                        fieldName + "은 필수입니다."
                );
            }

            return;
        }

        if (value.compareTo(ZERO) <= 0) {
            throw invalidRequest(
                    fieldName + "은 0보다 커야 합니다."
            );
        }

        BigDecimal normalizedValue =
                value.stripTrailingZeros();

        int fractionDigits = Math.max(
                normalizedValue.scale(),
                0
        );

        int integerDigits = Math.max(
                normalizedValue.precision()
                        - normalizedValue.scale(),
                0
        );

        if (integerDigits > AREA_MAX_INTEGER_DIGITS
                || fractionDigits > AREA_MAX_FRACTION_DIGITS) {
            throw invalidRequest(
                    fieldName
                            + "은 정수 8자리와 소수점 이하 2자리까지 입력할 수 있습니다."
            );
        }
    }

    private void validateTotalFloor(Integer totalFloor) {
        if (totalFloor != null && totalFloor <= 0) {
            throw invalidRequest(
                    "전체 층수는 0보다 커야 합니다."
            );
        }
    }

    private void validateRequiredText(
            String fieldName,
            String value
    ) {
        if (value == null || value.isBlank()) {
            throw invalidRequest(
                    fieldName + "은 필수입니다."
            );
        }
    }

    private void validateMaxLength(
            String fieldName,
            String value,
            int maxLength
    ) {
        if (value != null && value.length() > maxLength) {
            throw invalidRequest(
                    fieldName
                            + "은 "
                            + maxLength
                            + "자 이하여야 합니다."
            );
        }
    }

    private BusinessException invalidRequest(
            String message
    ) {
        return new BusinessException(
                CustomResponseCode.INVALID_REQUEST,
                message
        );
    }
}