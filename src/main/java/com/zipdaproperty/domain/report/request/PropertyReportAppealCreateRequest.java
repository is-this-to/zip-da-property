package com.zipdaproperty.domain.report.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PropertyReportAppealCreateRequest(

        @NotBlank(message = "이의신청 상세 내용은 필수입니다.")
        @Size(
                min = 20,
                max = 2000,
                message = "이의신청 상세 내용은 20자 이상 2000자 이하로 입력해주세요."
        )
        String detail
) {
}
