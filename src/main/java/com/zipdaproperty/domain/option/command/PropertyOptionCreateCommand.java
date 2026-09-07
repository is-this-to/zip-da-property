package com.zipdaproperty.domain.option.command;

public record PropertyOptionCreateCommand(
        String optionCode,
        String optionValue
) {
}
