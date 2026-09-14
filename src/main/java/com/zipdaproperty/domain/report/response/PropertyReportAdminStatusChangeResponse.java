package com.zipdaproperty.domain.report.response;

import com.zipdaproperty.domain.report.entity.PropertyReport;
import com.zipdaproperty.domain.report.type.ReportStatus;
import com.zipdaproperty.global.id.TsidString;

public record PropertyReportAdminStatusChangeResponse(
        @TsidString Long reportId,
        ReportStatus status,
        Long version
) {
    public static PropertyReportAdminStatusChangeResponse from(PropertyReport report) {
        return new PropertyReportAdminStatusChangeResponse(
                report.getReportId(),
                report.getStatus(),
                report.getVersion()
        );
    }
}
