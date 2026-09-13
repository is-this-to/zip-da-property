package com.zipdaproperty.domain.report.request;

import com.zipdaproperty.domain.report.type.ReportStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record PropertyReportAdminStatusChangeRequest(
        @NotNull(message = "targetStatus는 필수입니다.")
        ReportStatus targetStatus,
        @NotNull(message = "version은 필수입니다.")
        @PositiveOrZero(message = "version은 0 이상이어야 합니다.")
        Long version
) {
}
