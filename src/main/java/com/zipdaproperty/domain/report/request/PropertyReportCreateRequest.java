package com.zipdaproperty.domain.report.request;

import com.zipdaproperty.domain.report.type.ReportReasonCode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PropertyReportCreateRequest(

        @NotNull(message = "신고 사유는 필수입니다.")
        ReportReasonCode reasonCode,

        @NotBlank(message = "신고 상세 내용은 필수입니다.")
        @Size(
                min = 10,
                max = 1000,
                message = "신고 상세 내용은 10자 이상 1000자 이하로 입력해주세요."
        )
        String detail
) {
}
