package com.zipdaproperty.domain.report.repository;

import com.zipdaproperty.domain.report.type.ReportReasonCode;
import com.zipdaproperty.domain.report.type.ReportStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record PropertyReportAdminDetailQueryRow(
        Long reportId,
        Long propertyId,
        Long reporterMemberId,
        ReportReasonCode reasonCode,
        String detail,
        ReportStatus status,
        BigDecimal riskScore,
        Long assignedAdminId,
        Long version,
        Instant createdAt
) {
}
