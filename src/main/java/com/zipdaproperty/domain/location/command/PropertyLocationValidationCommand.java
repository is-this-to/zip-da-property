package com.zipdaproperty.domain.location.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

public record PropertyLocationValidationCommand(
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

    public PropertyLocationValidationCommand {
        if (legalDongCode != null) {
            legalDongCode = legalDongCode.trim();
        }
    }
}
