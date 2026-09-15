package com.zipdaproperty.domain.option.response;

import com.zipdaproperty.domain.option.type.OptionCategory;

public record PropertyEditOptionResponse(
        String optionCode,
        String optionName,
        OptionCategory optionCategory,
        String optionValue,
        boolean required,
        int displayOrder
) {
}
