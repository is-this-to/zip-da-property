package com.zipdaproperty.domain.report.response;

import com.zipdaproperty.domain.report.entity.PropertyReportAppeal;
import com.zipdaproperty.domain.report.type.AppealStatus;

public record PropertyReportAppealAdminReviewResponse(
        Long appealId,
        AppealStatus status,
        Long version
) {

    public static PropertyReportAppealAdminReviewResponse from(
            PropertyReportAppeal appeal
    ) {
        return new PropertyReportAppealAdminReviewResponse(
                appeal.getAppealId(),
                appeal.getStatus(),
                appeal.getVersion()
        );
    }
}
