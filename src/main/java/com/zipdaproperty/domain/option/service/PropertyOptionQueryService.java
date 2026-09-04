package com.zipdaproperty.domain.option.service;

import com.zipdaproperty.domain.option.entity.PropertyOptionCode;
import com.zipdaproperty.domain.option.entity.PropertyTypeOption;
import com.zipdaproperty.domain.option.repository.PropertyTypeOptionRepository;
import com.zipdaproperty.domain.option.response.PropertyOptionCodeListResponse;
import com.zipdaproperty.domain.option.response.PropertyOptionCodeResponse;
import com.zipdaproperty.domain.option.type.OptionValueType;
import com.zipdaproperty.domain.property.constant.PropertyType;
import com.zipdaproperty.global.error.custom.BusinessException;
import com.zipdaproperty.global.response.constant.CustomResponseCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PropertyOptionQueryService {

    private final PropertyTypeOptionRepository propertyTypeOptionRepository;
    private final JsonMapper jsonMapper;

    public PropertyOptionCodeListResponse getOptionCodes(
            PropertyType propertyType
    ) {
        List<PropertyTypeOption> typeOptions =
                propertyTypeOptionRepository
                        .findAllByPropertyTypeAndDeletedAtIsNullAndOptionCodeDeletedAtIsNullAndOptionCodeActiveTrueOrderByDisplayOrderAsc(
                                propertyType
                        );

        List<PropertyOptionCodeResponse> items = typeOptions.stream()
                .map(this::toResponse)
                .toList();

        return new PropertyOptionCodeListResponse(items);
    }

    private PropertyOptionCodeResponse toResponse(
            PropertyTypeOption typeOption
    ) {
        PropertyOptionCode optionCode = typeOption.getOptionCode();

        return new PropertyOptionCodeResponse(
                optionCode.getOptionCode(),
                optionCode.getOptionName(),
                optionCode.getOptionCategory(),
                optionCode.getValueType(),
                optionCode.getUnit(),
                getAllowedValues(optionCode),
                optionCode.isFilterable(),
                optionCode.isRegistrationEnabled(),
                typeOption.isRequired(),
                typeOption.getDisplayOrder()
        );
    }

    private List<String> getAllowedValues(
            PropertyOptionCode optionCode
    ) {
        if (optionCode.getValueType() != OptionValueType.SINGLE_SELECT) {
            return null;
        }

        String allowedValuesJson = optionCode.getAllowedValuesJson();

        if (allowedValuesJson == null || allowedValuesJson.isBlank()) {
            return List.of();
        }

        try {
            return jsonMapper.readValue(
                    allowedValuesJson,
                    new TypeReference<List<String>>() {}
            );
        } catch (Exception e) {
            throw new BusinessException(
                    CustomResponseCode.SYSTEM_ERROR,
                    "옵션 허용값 설정을 읽을 수 없습니다."
            );
        }
    }
}
