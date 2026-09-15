package com.zipdaproperty.domain.option.response;

import java.util.List;

public record PropertyOptionCodeListResponseDTO(
        List<PropertyOptionCodeResponseDTO> items
) {
    public PropertyOptionCodeListResponseDTO {
        items = List.copyOf(items);
    }
}
