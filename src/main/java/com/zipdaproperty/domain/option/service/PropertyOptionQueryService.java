package com.zipdaproperty.domain.option.service;

import com.zipdaproperty.domain.option.entity.PropertyOptionCode;
import com.zipdaproperty.domain.option.entity.PropertyTypeOption;
import com.zipdaproperty.domain.option.repository.PropertyOptionQueryDSLRepository;
import com.zipdaproperty.domain.option.response.PropertyOptionCodeListResponse;
import com.zipdaproperty.domain.option.response.PropertyOptionCodeResponse;
import com.zipdaproperty.domain.property.constant.PropertyType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PropertyOptionQueryService {

    private final PropertyOptionQueryDSLRepository queryRepository;

    public PropertyOptionCodeListResponse getOptionCodes(
            PropertyType propertyType
    ) {
        List<PropertyTypeOption> typeOptions =
                queryRepository
                        .findActiveTypeOptions(propertyType);

        if (typeOptions.isEmpty()) {
            return new PropertyOptionCodeListResponse(List.of());
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

        List<PropertyOptionCodeResponse> items = typeOptions.stream()
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

        return new PropertyOptionCodeListResponse(items);
    }

    private PropertyOptionCodeResponse toResponse(
            PropertyTypeOption typeOption,
            PropertyOptionCode optionCode
    ) {
        return new PropertyOptionCodeResponse(
                optionCode.getOptionCode(),
                optionCode.getOptionName(),
                optionCode.getOptionCategory(),
                optionCode.isFilterable(),
                optionCode.isRegistrationEnabled(),
                typeOption.isRequired(),
                typeOption.getDisplayOrder()
        );
    }

}
