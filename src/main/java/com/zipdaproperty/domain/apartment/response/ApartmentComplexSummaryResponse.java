package com.zipdaproperty.domain.apartment.response;

import com.zipdaproperty.global.id.TsidString;

import java.time.LocalDate;

public record ApartmentComplexSummaryResponse(
        @TsidString Long apartmentComplexId,
        String complexName,
        String roadAddress,
        String jibunAddress,
        Integer totalBuildings,
        Integer totalHouseholds,
        Integer totalParkingSpaces,
        LocalDate approvalDate
) {
}
