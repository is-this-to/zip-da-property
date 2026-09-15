package com.zipdaproperty.domain.property.response;

import com.zipdaproperty.domain.property.verification.constant.PropertyVerificationStatus;
import com.zipdaproperty.domain.property.verification.constant.PropertyVerificationType;
import com.zipdaproperty.global.id.TsidString;

import java.time.Instant;

public record PropertyPublicationAdminVerificationSummary(
    @TsidString Long verificationId,
    PropertyVerificationType verificationType,
    PropertyVerificationStatus status,
    Instant submittedAt,
    Instant reviewedAt,
    Instant verifiedAt,
    Instant expiresAt,
    String resultReason
) {}