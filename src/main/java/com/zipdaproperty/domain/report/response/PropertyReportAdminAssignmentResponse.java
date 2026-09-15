package com.zipdaproperty.domain.report.response;

import com.zipdaproperty.domain.report.entity.PropertyReport;
import com.zipdaproperty.global.id.TsidString;

public record PropertyReportAdminAssignmentResponse(
        @TsidString Long reportId,
        @TsidString Long assignedAdminId,
        Long version
) {
    public static PropertyReportAdminAssignmentResponse from(PropertyReport report) {
        return new PropertyReportAdminAssignmentResponse(
                report.getReportId(),
                report.getAssignedAdminId(),
                report.getVersion()
        );
    }
}
