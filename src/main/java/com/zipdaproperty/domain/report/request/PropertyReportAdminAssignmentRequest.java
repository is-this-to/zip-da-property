package com.zipdaproperty.domain.report.request;

import com.zipdaproperty.global.id.TsidString;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public record PropertyReportAdminAssignmentRequest(
        @TsidString
        @NotNull(message = "assignedAdminId는 필수입니다.")
        @Positive(message = "assignedAdminId는 0보다 커야 합니다.")
        Long assignedAdminId,
        @NotNull(message = "version은 필수입니다.")
        @PositiveOrZero(message = "version은 0 이상이어야 합니다.")
        Long version
) {
}
