package com.zipdaproperty.domain.property.verification.response;

import com.zipdaproperty.domain.property.constant.PropertyType;
import com.zipdaproperty.domain.property.constant.PublicationStatus;
import com.zipdaproperty.domain.property.constant.PublisherType;
import com.zipdaproperty.domain.property.constant.TransactionType;
import com.zipdaproperty.domain.property.constant.VerificationStatus;
import com.zipdaproperty.domain.property.response.PropertyEditAddressResponse;
import com.zipdaproperty.domain.property.verification.constant.PropertyVerificationStatus;
import com.zipdaproperty.domain.property.verification.constant.PropertyVerificationType;
import com.zipdaproperty.global.id.TsidString;

import java.time.Instant;
import java.util.List;

public record PropertyVerificationAdminDetailResponse(
    @TsidString Long propertyId,
    @TsidString Long verificationId,
    Long propertyVersion,
    Integer verificationVersion,

    String title,
    PublisherType publisherType,
    PropertyType propertyType,
    TransactionType transactionType,
    PublicationStatus publicationStatus,
    VerificationStatus verificationStatus,

    PropertyVerificationType verificationType,
    PropertyVerificationStatus status,

    @TsidString Long applicantMemberId,
    @TsidString Long reviewerMemberId,
    String reviewerRole,

    Instant submittedAt,
    Instant reviewedAt,
    Instant verifiedAt,
    Instant expiresAt,

    String resultCode,
    String resultReason,

    PropertyEditAddressResponse address,
    List<PropertyVerificationAdminEvidenceResponse> evidence
) {}