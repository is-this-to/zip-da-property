package com.zipdaproperty.domain.option.response;

import com.zipdaproperty.domain.option.type.OptionCategory;

public record PropertyDetailOptionResponse(
        String optionCode,
        String optionName,
        OptionCategory optionCategory,
        String optionValue,
        int displayOrder
) {
}
