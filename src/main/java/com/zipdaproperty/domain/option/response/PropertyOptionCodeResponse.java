package com.zipdaproperty.domain.option.response;

import com.zipdaproperty.domain.option.type.OptionCategory;

public record PropertyOptionCodeResponse(
        String optionCode,
        String optionName,
        OptionCategory optionCategory,
        boolean filterable,
        boolean registrationEnabled,
        boolean required,
        int displayOrder
) {
}
