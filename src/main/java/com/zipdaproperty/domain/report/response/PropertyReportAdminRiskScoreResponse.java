package com.zipdaproperty.domain.report.response;

import com.zipdaproperty.domain.report.entity.PropertyReport;
import com.zipdaproperty.global.id.TsidString;

import java.math.BigDecimal;

public record PropertyReportAdminRiskScoreResponse(
        @TsidString Long reportId,
        BigDecimal riskScore,
        Long version
) {
    public static PropertyReportAdminRiskScoreResponse from(PropertyReport report) {
        return new PropertyReportAdminRiskScoreResponse(
                report.getReportId(),
                report.getRiskScore(),
                report.getVersion()
        );
    }
}
