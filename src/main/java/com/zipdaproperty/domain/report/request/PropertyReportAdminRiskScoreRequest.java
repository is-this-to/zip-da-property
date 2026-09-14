package com.zipdaproperty.domain.report.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record PropertyReportAdminRiskScoreRequest(
        @NotNull(message = "riskScore는 필수입니다.")
        @DecimalMin(value = "0.00", message = "riskScore는 0 이상이어야 합니다.")
        @DecimalMax(value = "100.00", message = "riskScore는 100 이하여야 합니다.")
        @Digits(integer = 3, fraction = 2, message = "riskScore는 소수점 둘째 자리까지 입력할 수 있습니다.")
        BigDecimal riskScore,
        @NotNull(message = "version은 필수입니다.")
        @PositiveOrZero(message = "version은 0 이상이어야 합니다.")
        Long version
) {
}
