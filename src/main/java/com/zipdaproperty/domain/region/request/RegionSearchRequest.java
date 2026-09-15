package com.zipdaproperty.domain.region.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegionSearchRequest(
        @Schema(
                description = "검색할 지역명",
                example = "강남"
        )
        @NotBlank(
                message = "지역 검색어는 필수입니다."
        )
        @Size(
                min = 1,
                max = 50,
                message = "지역 검색어는 1자 이상 50자 이하여야 합니다."
        )
        String keyword
) {
    /*
     * 객체가 생성될 때 검색어 앞뒤의 공백을 제거한다.
     *
     * 예:
     * "  강남  " -> "강남"
     */
    public RegionSearchRequest{
        if(keyword != null){
            keyword = keyword.trim();
        }
    }
}
