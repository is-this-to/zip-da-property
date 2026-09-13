package com.zipdaproperty.domain.property.response;

import java.util.List;

public record PropertyPublicListResponse(
        List<PropertyPublicListItemResponse> items,
        String nextCursor,
        boolean hasNext
) {
    public PropertyPublicListResponse {
        items = List.copyOf(items);
    }
}
