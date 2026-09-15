package com.zipdaproperty.domain.option.service;

import com.zipdaproperty.domain.option.entity.PropertyOptionCode;
import com.zipdaproperty.domain.option.entity.PropertyTypeOption;
import com.zipdaproperty.domain.option.repository.PropertyEditOptionQueryRow;
import com.zipdaproperty.domain.option.repository.PropertyOptionQueryDSLRepository;
import com.zipdaproperty.domain.option.response.PropertyOptionCodeListResponseDTO;
import com.zipdaproperty.domain.option.response.PropertyOptionCodeResponseDTO;
import com.zipdaproperty.domain.option.response.PropertyDetailOptionResponse;
import com.zipdaproperty.domain.option.response.PropertyEditOptionResponse;
import com.zipdaproperty.domain.property.constant.PropertyType;
import com.zipdaproperty.global.error.custom.BusinessException;
import com.zipdaproperty.global.response.constant.CustomResponseCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PropertyOptionQueryService {

    private final PropertyOptionQueryDSLRepository queryRepository;

    public PropertyOptionCodeListResponseDTO getOptionCodes(
            PropertyType propertyType
    ) {
        List<PropertyTypeOption> typeOptions =
                queryRepository
                        .findActiveTypeOptions(propertyType);

        if (typeOptions.isEmpty()) {
            return new PropertyOptionCodeListResponseDTO(List.of());
        }

        List<Long> optionCodeIds = typeOptions.stream()
                .map(PropertyTypeOption::getOptionCodeId)
                .distinct()
                .toList();

        Map<Long, PropertyOptionCode> optionCodeById =
                queryRepository
                        .findActiveOptionCodesByIds(
                                optionCodeIds
                        )
                        .stream()
                        .collect(Collectors.toMap(
                                PropertyOptionCode::getOptionCodeId,
                                Function.identity()
                        ));

        List<PropertyOptionCodeResponseDTO> items = typeOptions.stream()
                .filter(typeOption ->
                        optionCodeById.containsKey(typeOption.getOptionCodeId())
                )
                .sorted(
                        Comparator.comparingInt(PropertyTypeOption::getDisplayOrder)
                                .thenComparing(typeOption ->
                                        optionCodeById
                                                .get(typeOption.getOptionCodeId())
                                                .getOptionCode()
                                )
                                .thenComparing(
                                        PropertyTypeOption::getPropertyTypeOptionId
                                )
                )
                .map(typeOption -> toResponse(
                        typeOption,
                        optionCodeById.get(typeOption.getOptionCodeId())
                ))
                .toList();

        return new PropertyOptionCodeListResponseDTO(items);
    }

    public List<PropertyEditOptionResponse> getEditOptions(
            Long propertyId,
            PropertyType propertyType
    ) {
        Map<String, PropertyEditOptionQueryRow> optionByCode =
                queryRepository.findEditOptions(propertyId, propertyType)
                .stream()
                .collect(Collectors.toMap(
                        PropertyEditOptionQueryRow::optionCode,
                        Function.identity(),
                        (first, ignored) -> {
                            throw duplicatedEditOption(
                                    propertyId,
                                    propertyType,
                                    first.optionCode()
                            );
                        },
                        LinkedHashMap::new
                ));

        return optionByCode.values().stream()
                .map(option -> toEditOptionResponse(
                        propertyId,
                        propertyType,
                        option
                ))
                .toList();
    }

    public List<PropertyDetailOptionResponse> getDetailVisibleOptions(
            Long propertyId
    ) {
        return queryRepository.findDetailVisibleOptions(propertyId);
    }

    private PropertyOptionCodeResponseDTO toResponse(
            PropertyTypeOption typeOption,
            PropertyOptionCode optionCode
    ) {
        return new PropertyOptionCodeResponseDTO(
                optionCode.getOptionCode(),
                optionCode.getOptionName(),
                optionCode.getOptionCategory(),
                optionCode.isFilterable(),
                optionCode.isRegistrationEnabled(),
                typeOption.isRequired(),
                typeOption.getDisplayOrder()
        );
    }

    private PropertyEditOptionResponse toEditOptionResponse(
            Long propertyId,
            PropertyType propertyType,
            PropertyEditOptionQueryRow option
    ) {
        if (option.required() == null) {
            throw new BusinessException(
                    CustomResponseCode.SYSTEM_ERROR,
                    "활성 옵션의 유형별 매핑이 없습니다: propertyId = "
                            + propertyId
                            + ", propertyType = "
                            + propertyType
                            + ", optionCode = "
                            + option.optionCode()
            );
        }

        return new PropertyEditOptionResponse(
                option.optionCode(),
                option.optionName(),
                option.optionCategory(),
                option.optionValue(),
                option.required(),
                option.displayOrder()
        );
    }

    private BusinessException duplicatedEditOption(
            Long propertyId,
            PropertyType propertyType,
            String optionCode
    ) {
        return new BusinessException(
                CustomResponseCode.SYSTEM_ERROR,
                "수정 상세 옵션 조회 결과가 중복되었습니다: propertyId = "
                        + propertyId
                        + ", propertyType = "
                        + propertyType
                        + ", optionCode = "
                        + optionCode
        );
    }

}
