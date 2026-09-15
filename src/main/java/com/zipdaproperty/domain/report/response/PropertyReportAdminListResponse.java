package com.zipdaproperty.domain.report.response;

import java.util.List;

public record PropertyReportAdminListResponse(
        List<PropertyReportAdminListItemResponse> items,
        String nextCursor,
        boolean hasNext
) {
    public PropertyReportAdminListResponse {
        items = List.copyOf(items);
    }
}
