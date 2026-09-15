package com.zipdaproperty.domain.property.verification.response;

import com.zipdaproperty.domain.property.constant.VerificationStatus;
import com.zipdaproperty.domain.property.verification.constant.PropertyVerificationStatus;
import com.zipdaproperty.domain.property.verification.constant.PropertyVerificationType;
import com.zipdaproperty.global.id.TsidString;

import java.time.Instant;

public record PropertyVerificationAdminListItemResponse(
        @TsidString Long propertyId,
        @TsidString Long verificationId,
        Long propertyVersion,
        Integer verificationVersion,
        PropertyVerificationType verificationType,
        PropertyVerificationStatus status,
        VerificationStatus verificationStatus,
        Instant submittedAt
) {}
