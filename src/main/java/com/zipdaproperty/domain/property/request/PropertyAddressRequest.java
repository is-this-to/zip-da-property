package com.zipdaproperty.domain.property.request;

import com.zipdaproperty.domain.property.command.PropertyAddressCommand;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record PropertyAddressRequest(
        @Size(max = 300, message = "도로명 주소는 300자 이하여야 합니다.")
        String roadAddress,

        @Size(max = 300, message = "지번 주소는 300자 이하여야 합니다.")
        String jibunAddress,

        @NotBlank(message = "법정동 코드는 필수입니다.")
        @Pattern(
                regexp = "^\\d{10}$",
                message = "법정동 코드는 10자리 숫자여야 합니다."
        )
        String legalDongCode,

        @NotNull(message = "경도는 필수입니다.")
        BigDecimal longitude,

        @NotNull(message = "위도는 필수입니다.")
        BigDecimal latitude
) {

    public PropertyAddressRequest {
        roadAddress = trimToNull(roadAddress);
        jibunAddress = trimToNull(jibunAddress);
        legalDongCode = trimToNull(legalDongCode);
    }

    @AssertTrue(message = "도로명 주소 또는 지번 주소 중 하나는 필수입니다.")
    public boolean isAddressPresent() {
        return roadAddress != null || jibunAddress != null;
    }

    public PropertyAddressCommand toCommand() {
        return new PropertyAddressCommand(
                roadAddress,
                jibunAddress,
                legalDongCode,
                longitude,
                latitude
        );
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
