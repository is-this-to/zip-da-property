package com.zipdaproperty.domain.report.response;

import com.zipdaproperty.domain.report.type.ReportEvidenceType;
import com.zipdaproperty.global.id.TsidString;

public record PropertyReportAdminEvidenceResponse(
        Long evidenceId,
        @TsidString Long fileId,
        ReportEvidenceType evidenceType,
        Integer sortOrder,
        String fileUrl
) {
}
