package com.zipdaproperty.domain.property.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record PropertyMapBoundsResponse(

        @Schema(
                description = "현재 응답에 포함된 공개 매물 목록"
        )
        List<PropertyMapBoundsItemResponse> items,

        @Schema(
                description = "조회 조건에 해당하는 전체 매물 수",
                example = "640"
        )
        long totalCount,

        @Schema(
                description = "전체 결과가 최대 응답 개수인 500개를 초과했는지 여부",
                example = "true"
        )
        boolean truncated

) {

    public PropertyMapBoundsResponse {
        items = List.copyOf(items);
    }
}
