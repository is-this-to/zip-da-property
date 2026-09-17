package com.zipdaproperty.domain.property.response;

import java.util.List;

public record PropertyPublicationAdminListResponse(
        List<PropertyPublicationAdminListItemResponse> items,
        String nextCursor,
        boolean hasNext
) {}
