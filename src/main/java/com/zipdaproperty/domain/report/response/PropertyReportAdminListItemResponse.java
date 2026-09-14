package com.zipdaproperty.domain.report.response;

import com.zipdaproperty.domain.report.type.ReportReasonCode;
import com.zipdaproperty.domain.report.type.ReportStatus;
import com.zipdaproperty.global.id.TsidString;

import java.math.BigDecimal;
import java.time.Instant;

public record PropertyReportAdminListItemResponse(
        @TsidString Long reportId,
        @TsidString Long propertyId,
        @TsidString Long reporterMemberId,
        ReportReasonCode reasonCode,
        ReportStatus status,
        BigDecimal riskScore,
        @TsidString Long assignedAdminId,
        Instant createdAt
) {
}
