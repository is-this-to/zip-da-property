package com.zipdaproperty.global.config.location;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;

@Validated
@ConfigurationProperties(prefix = "property.location.validation")
public record PropertyLocationValidationProperties(
        @NotNull BigDecimal minLongitude,
        @NotNull BigDecimal maxLongitude,
        @NotNull BigDecimal minLatitude,
        @NotNull BigDecimal maxLatitude
) {

    public PropertyLocationValidationProperties {
        if (minLongitude != null
                && maxLongitude != null
                && minLongitude.compareTo(maxLongitude) >= 0) {
            throw new IllegalArgumentException(
                    "최소 경도는 최대 경도보다 작아야 합니다."
            );
        }

        if (minLatitude != null
                && maxLatitude != null
                && minLatitude.compareTo(maxLatitude) >= 0) {
            throw new IllegalArgumentException(
                    "최소 위도는 최대 위도보다 작아야 합니다."
            );
        }
    }

    public boolean contains(
            BigDecimal longitude,
            BigDecimal latitude
    ) {
        if (longitude == null || latitude == null) {
            return false;
        }

        boolean validLongitude =
                longitude.compareTo(minLongitude) >= 0
                        && longitude.compareTo(maxLongitude) <= 0;

        boolean validLatitude =
                latitude.compareTo(minLatitude) >= 0
                        && latitude.compareTo(maxLatitude) <= 0;

        return validLongitude && validLatitude;
    }
}
