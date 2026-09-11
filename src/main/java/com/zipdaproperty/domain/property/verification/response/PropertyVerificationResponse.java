package com.zipdaproperty.domain.property.verification.response;

import com.zipdaproperty.domain.property.constant.VerificationStatus;
import com.zipdaproperty.domain.property.entity.Property;
import com.zipdaproperty.domain.property.verification.constant.PropertyVerificationStatus;
import com.zipdaproperty.domain.property.verification.constant.PropertyVerificationType;
import com.zipdaproperty.domain.property.verification.entity.PropertyVerification;
import com.zipdaproperty.global.id.TsidString;

import java.time.Instant;

public record PropertyVerificationResponse(
        @TsidString Long propertyVerificationId,
        @TsidString Long propertyId,
        Long version,
        PropertyVerificationType verificationType,
        PropertyVerificationStatus verificationRequestStatus,
        VerificationStatus verificationStatus,
        Instant submittedAt,
        Instant reviewedAt,
        Instant verifiedAt,
        Instant expiresAt,
        String resultCode,
        String resultReason
) {
    public static PropertyVerificationResponse from(
            PropertyVerification verification,
            Property property
    ) {
        return new PropertyVerificationResponse(
                verification.getPropertyVerificationId(),
                property.getPropertyId(),
                property.getVersion(),
                verification.getVerificationType(),
                verification.getStatus(),
                property.getVerificationStatus(),
                verification.getSubmittedAt(),
                verification.getReviewedAt(),
                verification.getVerifiedAt(),
                verification.getExpiresAt(),
                verification.getResultCode(),
                verification.getResultReason()
        );
    }
}
