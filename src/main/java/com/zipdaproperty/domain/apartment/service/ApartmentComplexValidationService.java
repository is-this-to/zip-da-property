package com.zipdaproperty.domain.apartment.service;

import com.zipdaproperty.domain.apartment.entity.ApartmentComplex;
import com.zipdaproperty.domain.apartment.repository.ApartmentComplexRepository;
import com.zipdaproperty.domain.property.constant.PropertyType;
import com.zipdaproperty.global.error.custom.BusinessException;
import com.zipdaproperty.global.response.constant.CustomResponseCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ApartmentComplexValidationService {

    private final ApartmentComplexRepository apartmentComplexRepository;

    public void validateForProperty(
            PropertyType propertyType,
            Long apartmentComplexId,
            Long verifiedRegionId
    ) {
        if (propertyType == null) {
            throw invalidRequest("매물 유형은 필수입니다.");
        }

        if (propertyType != PropertyType.APARTMENT) {
            if (apartmentComplexId != null) {
                throw invalidRequest(
                        "아파트가 아닌 매물에는 아파트 단지 ID를 지정할 수 없습니다."
                );
            }
            return;
        }

        if (apartmentComplexId == null || apartmentComplexId <= 0) {
            throw invalidRequest("아파트 매물은 단지 선택이 필수입니다.");
        }

        ApartmentComplex apartmentComplex = apartmentComplexRepository
                .findByApartmentComplexIdAndIsActiveTrueAndDeletedAtIsNull(apartmentComplexId)
                .orElseThrow(() -> new BusinessException(
                        CustomResponseCode.NOT_FOUND_RESOURCE,
                        "활성 상태의 아파트 단지를 찾을 수 없습니다. apartmentComplexId = "
                                + apartmentComplexId
                ));

        if (!Objects.equals(apartmentComplex.getRegionId(), verifiedRegionId)) {
            throw new BusinessException(
                    CustomResponseCode.PROPERTY_LOCATION_REGION_MISMATCH,
                    "선택한 아파트 단지와 검증된 주소의 Region이 일치하지 않습니다."
            );
        }
    }

    private BusinessException invalidRequest(String message) {
        return new BusinessException(CustomResponseCode.INVALID_REQUEST, message);
    }
}
