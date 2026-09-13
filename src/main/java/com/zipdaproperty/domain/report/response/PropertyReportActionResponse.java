package com.zipdaproperty.domain.report.response;

import com.zipdaproperty.domain.report.entity.PropertyReportAction;
import com.zipdaproperty.domain.report.type.ReportActionCode;
import com.zipdaproperty.global.id.TsidString;

import java.time.Instant;

public record PropertyReportActionResponse(
        Long actionId,
        @TsidString Long reportId,
        @TsidString Long propertyId,
        ReportActionCode actionCode,
        Instant executedAt
) {

    public static PropertyReportActionResponse from(
            PropertyReportAction action
    ) {
        return new PropertyReportActionResponse(
                action.getActionId(),
                action.getReportId(),
                action.getPropertyId(),
                action.getActionCode(),
                action.getExecutedAt()
        );
    }
}
