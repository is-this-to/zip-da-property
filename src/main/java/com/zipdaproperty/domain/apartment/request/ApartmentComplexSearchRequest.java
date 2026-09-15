package com.zipdaproperty.domain.apartment.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ApartmentComplexSearchRequest(
        @Schema(description = "검증된 내부 Region ID", example = "53390")
        @NotNull(message = "Region ID는 필수입니다.")
        @Min(value = 1, message = "Region ID는 1 이상이어야 합니다.")
        Long regionId,

        @Schema(description = "단지명·도로명·지번 주소 검색어", example = "래미안")
        @Size(max = 100, message = "단지 검색어는 100자 이하여야 합니다.")
        String keyword,

        @Schema(description = "최대 조회 건수", example = "20")
        @Min(value = 1, message = "조회 건수는 1 이상이어야 합니다.")
        @Max(value = 50, message = "조회 건수는 50 이하여야 합니다.")
        Integer size
) {

    private static final int DEFAULT_SIZE = 20;

    public ApartmentComplexSearchRequest {
        keyword = normalizeNullable(keyword);
        size = size == null ? DEFAULT_SIZE : size;
    }

    private static String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().replaceAll("\\s+", " ");
        return normalized.isEmpty() ? null : normalized;
    }
}
