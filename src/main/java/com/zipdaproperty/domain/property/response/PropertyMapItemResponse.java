package com.zipdaproperty.domain.property.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
        description = "지도 확대 수준에 따라 구조가 달라지는 지도 응답 항목",
        oneOf = {
                PropertyMapRegionAggregateItemResponse.class,
                PropertyMapBoundsItemResponse.class
        }
)
public sealed interface PropertyMapItemResponse
        permits PropertyMapRegionAggregateItemResponse,
                PropertyMapBoundsItemResponse {
}
