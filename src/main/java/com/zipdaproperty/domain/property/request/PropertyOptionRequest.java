package com.zipdaproperty.domain.property.request;

import com.zipdaproperty.domain.option.command.PropertyOptionCreateCommand;

public record PropertyOptionRequest(
        String optionCode,
        String optionValue
) {
    public PropertyOptionCreateCommand toCommand() {
        return new PropertyOptionCreateCommand(optionCode, optionValue);
    }
}
