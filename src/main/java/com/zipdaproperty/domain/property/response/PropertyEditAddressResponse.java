package com.zipdaproperty.domain.property.response;

import com.zipdaproperty.domain.property.entity.PropertyAddress;
import org.locationtech.jts.geom.Point;

import java.math.BigDecimal;

public record PropertyEditAddressResponse(
        String roadAddress,
        String jibunAddress,
        String legalDongCode,
        BigDecimal longitude,
        BigDecimal latitude
) {

    public static PropertyEditAddressResponse from(PropertyAddress address) {
        Point exactLocation = address.getExactLocation();
        return new PropertyEditAddressResponse(
                address.getExactRoadAddress(),
                address.getExactJibunAddress(),
                address.getLegalDongCode(),
                BigDecimal.valueOf(exactLocation.getX()),
                BigDecimal.valueOf(exactLocation.getY())
        );
    }
}
