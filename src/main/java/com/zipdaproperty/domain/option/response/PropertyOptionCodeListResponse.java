package com.zipdaproperty.domain.option.response;

import java.util.List;

public record PropertyOptionCodeListResponse(
        List<PropertyOptionCodeResponse> items
) {
    public PropertyOptionCodeListResponse {
        items = List.copyOf(items);
    }
}
