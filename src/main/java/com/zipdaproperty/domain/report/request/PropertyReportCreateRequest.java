package com.zipdaproperty.domain.report.request;

import com.zipdaproperty.domain.report.type.ReportReasonCode;
import com.zipdaproperty.global.id.TsidLongDeserializer;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;

import java.util.List;

public record PropertyReportCreateRequest(

        @NotNull(message = "신고 사유는 필수입니다.")
        ReportReasonCode reasonCode,

        @NotBlank(message = "신고 상세 내용은 필수입니다.")
        @Size(
                min = 10,
                max = 1000,
                message = "신고 상세 내용은 10자 이상 1000자 이하로 입력해주세요."
        )
        String detail,

        @Size(max = 5, message = "신고 증빙 파일은 최대 5개까지 등록할 수 있습니다.")
        @JsonDeserialize(contentUsing = TsidLongDeserializer.class)
        @JsonSerialize(contentUsing = ToStringSerializer.class)
        List<@NotNull(message = "증빙 파일 ID는 null일 수 없습니다.") Long> evidenceFileIds
) {
}
