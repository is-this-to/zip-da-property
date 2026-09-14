package com.zipdaproperty.domain.report.response;

import com.zipdaproperty.domain.report.type.ReportReasonCode;
import com.zipdaproperty.domain.report.type.ReportStatus;
import com.zipdaproperty.global.id.TsidString;

import java.time.Instant;

public record PropertyReportMyListItemResponse(
        @TsidString Long reportId,
        @TsidString Long propertyId,
        ReportReasonCode reasonCode,
        ReportStatus status,
        Instant createdAt
) {
}
