package com.zipdaproperty.domain.property.response;

import com.zipdaproperty.domain.property.constant.PropertyMapResponseType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record PropertyMapBoundsResponse(

        @Schema(
                description = """
                        현재 지도 응답 유형.
                        REGION_AGGREGATE: 지역 집계
                        PROPERTY_POINTS: 프런트 좌표 클러스터링
                        PROPERTY_MARKER: 개별 가격 마커
                        """,
                example = "REGION_AGGREGATE"
        )
        PropertyMapResponseType responseType,

        @Schema(description = "현재 확대 단계에 해당하는 지도 항목")
        List<PropertyMapItemResponse> items,

        @Schema(
                description = "현재 bounds에 포함된 공개 매물 전체 개수",
                example = "640"
        )
        long totalCount,

        @Schema(
                description = """
                        개별 좌표 결과가 최대 500개를 초과했는지 여부.
                        지역 집계 응답에서는 false입니다.
                        """,
                example = "true"
        )
        boolean truncated

) {

    public PropertyMapBoundsResponse {
        items = List.copyOf(items);
    }

    public static PropertyMapBoundsResponse of(
            PropertyMapResponseType responseType,
            List<? extends PropertyMapItemResponse> items,
            long totalCount,
            boolean truncated
    ) {
        return new PropertyMapBoundsResponse(
                responseType,
                List.copyOf(items),
                totalCount,
                truncated
        );
    }
}
