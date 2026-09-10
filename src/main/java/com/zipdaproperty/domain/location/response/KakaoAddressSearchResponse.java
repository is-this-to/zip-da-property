package com.zipdaproperty.domain.location.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "카카오 주소 검색 결과")
public record KakaoAddressSearchResponse(
        @Schema(description = "주소 검색 결과 목록")
        List<Item> items
) {

    public KakaoAddressSearchResponse {
        items = items == null
                ? List.of()
                : List.copyOf(items);
    }

    @Schema(description = "주소 검색 결과 항목")
    public record Item(
            @Schema(
                    description = "도로명 주소",
                    example = "대구 수성구 달구벌대로 2450"
            )
            String roadAddress,

            @Schema(
                    description = "지번 주소",
                    example = "대구광역시 수성구 범어동 123"
            )
            String jibunAddress,

            @Schema(
                    description = "법정동 코드",
                    example = "2726010100"
            )
            String legalDongCode,

            @Schema(
                    description = "건물명",
                    example = "범어빌딩"
            )
            String buildingName,

            @Schema(
                    description = "경도",
                    example = "128.625123"
            )
            BigDecimal longitude,

            @Schema(
                    description = "위도",
                    example = "35.859321"
            )
            BigDecimal latitude
    ) {
    }
}
