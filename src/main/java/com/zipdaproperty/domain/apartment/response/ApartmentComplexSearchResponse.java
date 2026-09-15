package com.zipdaproperty.domain.apartment.response;

import java.util.List;

public record ApartmentComplexSearchResponse(List<ApartmentComplexSummaryResponse> items) {

    public ApartmentComplexSearchResponse {
        items = items == null ? List.of() : List.copyOf(items);
    }
}
