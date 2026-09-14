package com.zipdaproperty.domain.report.repository;

import com.zipdaproperty.domain.report.type.ReportReasonCode;
import com.zipdaproperty.domain.report.type.ReportStatus;

import java.time.Instant;

public record PropertyReportMyListQueryRow(
        Long reportId,
        Long propertyId,
        ReportReasonCode reasonCode,
        ReportStatus status,
        Instant createdAt
) {
}
