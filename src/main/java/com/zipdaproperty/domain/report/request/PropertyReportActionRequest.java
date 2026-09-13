package com.zipdaproperty.domain.report.request;

import com.zipdaproperty.domain.report.type.ReportActionCode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PropertyReportActionRequest(
        @NotNull(message = "actionCode는 필수입니다.")
        ReportActionCode actionCode,

        @NotBlank(message = "reason은 필수입니다.")
        @Size(
                max = 1000,
                message = "reason은 1000자 이하여야 합니다."
        )
        String reason
) {
}
