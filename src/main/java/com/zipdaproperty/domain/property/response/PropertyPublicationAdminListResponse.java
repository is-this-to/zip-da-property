package com.zipdaproperty.domain.property.response;

import java.util.List;

public record PropertyPublicationAdminListResponse(
    List<PropertyPublicationAdminListItemResponse> content,
    int page,
    int size,
    long totalElements,
    int totalPages
) {}
