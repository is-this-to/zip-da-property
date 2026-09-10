package com.zipdaproperty.domain.property.request;

import com.zipdaproperty.domain.property.model.PropertyMapBounds;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record PropertyMapBoundsRequest(

        @Schema(
                description = "지도 남서쪽 위도",
                example = "37.45"
        )
        @NotNull(message = "minLat은 필수입니다.")
        @DecimalMin(
                value = "-90.0",
                message = "minLat은 -90 이상이어야 합니다."
        )
        @DecimalMax(
                value = "90.0",
                message = "minLat은 90 이하여야 합니다."
        )
        Double minLat,

        @Schema(
                description = "지도 북동쪽 위도",
                example = "37.55"
        )
        @NotNull(message = "maxLat은 필수입니다.")
        @DecimalMin(
                value = "-90.0",
                message = "maxLat은 -90 이상이어야 합니다."
        )
        @DecimalMax(
                value = "90.0",
                message = "maxLat은 90 이하여야 합니다."
        )
        Double maxLat,

        @Schema(
                description = "지도 남서쪽 경도",
                example = "126.95"
        )
        @NotNull(message = "minLng는 필수입니다.")
        @DecimalMin(
                value = "-180.0",
                message = "minLng는 -180 이상이어야 합니다."
        )
        @DecimalMax(
                value = "180.0",
                message = "minLng는 180 이하여야 합니다."
        )
        Double minLng,

        @Schema(
                description = "지도 북동쪽 경도",
                example = "127.10"
        )
        @NotNull(message = "maxLng는 필수입니다.")
        @DecimalMin(
                value = "-180.0",
                message = "maxLng는 -180 이상이어야 합니다."
        )
        @DecimalMax(
                value = "180.0",
                message = "maxLng는 180 이하여야 합니다."
        )
        Double maxLng,

        @Schema(
                description = "Kakao ROADMAP 확대 수준. 1은 가장 확대, 14는 가장 축소",
                example = "6"
        )
        @NotNull(message = "zoomLevel은 필수입니다.")
        @Min(
                value = 1,
                message = "zoomLevel은 1 이상이어야 합니다."
        )
        @Max(
                value = 14,
                message = "zoomLevel은 14 이하여야 합니다."
        )
        Integer zoomLevel

) {

    public PropertyMapBounds toBounds() {
        return new PropertyMapBounds(
                minLat,
                maxLat,
                minLng,
                maxLng,
                zoomLevel
        );
    }
}
