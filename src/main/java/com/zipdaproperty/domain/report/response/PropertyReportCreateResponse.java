package com.zipdaproperty.domain.report.response;

import com.zipdaproperty.domain.report.type.ReportStatus;
import com.zipdaproperty.global.id.TsidString;

public record PropertyReportCreateResponse(

        @TsidString
        Long reportId,

        ReportStatus status,

        Long version
) {
}