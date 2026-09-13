package com.zipdaproperty.domain.option.repository;

import com.zipdaproperty.domain.option.type.OptionCategory;

public record PropertyEditOptionQueryRow(
        String optionCode,
        String optionName,
        OptionCategory optionCategory,
        String optionValue,
        Boolean required,
        int displayOrder
) {
}
