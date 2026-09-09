package com.zipdaproperty.domain.property.response;

import java.util.List;

public record PropertyMyListResponse(

        List<PropertyMyListItemResponse> items,

        String nextCursor,

        boolean hasNext

) {

    public PropertyMyListResponse {
        items = List.copyOf(items);
    }
}