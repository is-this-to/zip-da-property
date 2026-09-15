package com.zipdaproperty.domain.location.response;

import com.zipdaproperty.domain.location.model.VerifiedPropertyLocation;
import com.zipdaproperty.domain.location.request.PropertyLocationValidationRequest;
import com.zipdaproperty.domain.region.entity.Region;
import com.zipdaproperty.global.id.TsidString;

import java.math.BigDecimal;

public record PropertyLocationValidationResponse(
        @TsidString Long regionId,
        String regionCode,
        String regionName,
        String fullRegionName,
        String roadAddress,
        String jibunAddress,
        String legalDongCode,
        BigDecimal longitude,
        BigDecimal latitude
) {

    public static PropertyLocationValidationResponse of(
            PropertyLocationValidationRequest request,
            VerifiedPropertyLocation verifiedLocation,
            Region region
    ) {
        return new PropertyLocationValidationResponse(
                verifiedLocation.regionId(),
                region.getRegionCode(),
                region.getRegionName(),
                region.getFullRegionName(),
                request.roadAddress(),
                request.jibunAddress(),
                verifiedLocation.legalDongCode(),
                request.longitude(),
                request.latitude()
        );
    }
}
