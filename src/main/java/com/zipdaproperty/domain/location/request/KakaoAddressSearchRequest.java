package com.zipdaproperty.domain.location.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record KakaoAddressSearchRequest(
        @Schema(
                description = "검색할 도로명 또는 지번 주소",
                example = "대구광역시 수성구 범어동"
        )
        @NotBlank(message = "주소 검색어는 필수입니다.")
        @Size(
                min = 1,
                max = 100,
                message = "주소 검색어는 1자 이상 100자 이하로 입력해야 합니다."
        )
        String query
) {

    public KakaoAddressSearchRequest {
        if (query != null) {
            query = query.trim();
        }
    }
}
