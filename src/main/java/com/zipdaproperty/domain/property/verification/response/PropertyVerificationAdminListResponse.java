package com.zipdaproperty.domain.property.verification.response;

import java.util.List;

public record PropertyVerificationAdminListResponse(
        List<PropertyVerificationAdminListItemResponse> items,
        String nextCursor,
        boolean hasNext
) {}
