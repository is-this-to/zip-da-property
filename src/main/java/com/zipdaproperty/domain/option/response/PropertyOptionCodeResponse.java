package com.zipdaproperty.domain.option.response;

import com.zipdaproperty.domain.option.type.OptionCategory;
import com.zipdaproperty.domain.option.type.OptionValueType;

import java.util.List;

public record PropertyOptionCodeResponse(
        String optionCode,
        String optionName,
        OptionCategory optionCategory,
        OptionValueType valueType,
        String unit,
        List<String> allowedValues,
        boolean filterable,
        boolean registrationEnabled,
        boolean required,
        int displayOrder
) {
}