package com.zipdaproperty.global.config.location;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.nio.charset.StandardCharsets;

@Validated
@ConfigurationProperties(prefix = "property.location.public-location")
public record PropertyPublicLocationProperties(
        @NotBlank String secret,
        @NotBlank String secretVersion,
        @Min(1) int minDistanceMeters,
        @Min(1) int maxDistanceMeters
) {

    private static final int MINIMUM_SECRET_LENGTH_BYTES = 32;

    public PropertyPublicLocationProperties {
        if (secret != null
                && secret.getBytes(StandardCharsets.UTF_8).length
                < MINIMUM_SECRET_LENGTH_BYTES) {
            throw new IllegalArgumentException(
                    "공개 위치 생성 비밀키는 32바이트 이상이어야 합니다."
            );
        }

        if (minDistanceMeters > maxDistanceMeters) {
            throw new IllegalArgumentException(
                    "공개 위치 최소 이동 거리는 최대 이동 거리보다 클 수 없습니다."
            );
        }
    }
}
