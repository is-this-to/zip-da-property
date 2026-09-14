package com.zipdaproperty.domain.report.response;

import java.util.List;

public record PropertyReportMyListResponse(
        List<PropertyReportMyListItemResponse> items,
        String nextCursor,
        boolean hasNext
) {
    public PropertyReportMyListResponse {
        items = List.copyOf(items);
    }
}
