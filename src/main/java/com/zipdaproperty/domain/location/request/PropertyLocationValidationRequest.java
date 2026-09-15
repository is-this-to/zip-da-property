package com.zipdaproperty.domain.location.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record PropertyLocationValidationRequest(
        @Schema(description = "도로명 주소", example = "서울특별시 강남구 테헤란로 123")
        @Size(max = 300, message = "도로명 주소는 300자 이하여야 합니다.")
        String roadAddress,

        @Schema(description = "지번 주소", example = "서울특별시 강남구 역삼동 123")
        @Size(max = 300, message = "지번 주소는 300자 이하여야 합니다.")
        String jibunAddress,

        @Schema(description = "10자리 법정동 코드", example = "1168010100")
        @NotBlank(message = "법정동 코드는 필수입니다.")
        @Pattern(regexp = "^\\d{10}$", message = "법정동 코드는 10자리 숫자여야 합니다.")
        String legalDongCode,

        @Schema(description = "경도", example = "127.012345")
        @NotNull(message = "경도는 필수입니다.")
        BigDecimal longitude,

        @Schema(description = "위도", example = "37.512345")
        @NotNull(message = "위도는 필수입니다.")
        BigDecimal latitude
) {

    public PropertyLocationValidationRequest {
        roadAddress = normalizeNullable(roadAddress);
        jibunAddress = normalizeNullable(jibunAddress);
        legalDongCode = normalizeNullable(legalDongCode);
    }

    @AssertTrue(message = "도로명 주소 또는 지번 주소 중 하나는 필수입니다.")
    public boolean isAddressPresent() {
        return roadAddress != null || jibunAddress != null;
    }

    private static String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().replaceAll("\\s+", " ");
        return normalized.isEmpty() ? null : normalized;
    }
}
