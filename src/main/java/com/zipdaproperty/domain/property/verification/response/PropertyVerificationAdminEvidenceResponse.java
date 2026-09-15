package com.zipdaproperty.domain.property.verification.response;

import com.zipdaproperty.domain.property.verification.constant.PropertyVerificationEvidenceType;
import com.zipdaproperty.global.id.TsidString;

public record PropertyVerificationAdminEvidenceResponse(
        @TsidString Long evidenceId,
        @TsidString Long propertyFileId,
        PropertyVerificationEvidenceType evidenceType,
        Integer sortOrder,
        String downloadUrl
) {}
