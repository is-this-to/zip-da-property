package com.zipdaproperty.domain.property.response;

import com.zipdaproperty.domain.property.constant.PropertyType;
import com.zipdaproperty.domain.property.constant.PublicationStatus;
import com.zipdaproperty.domain.property.constant.PublisherType;
import com.zipdaproperty.domain.property.constant.VerificationStatus;
import com.zipdaproperty.global.id.TsidString;

import java.time.Instant;

public record PropertyPublicationAdminListItemResponse(
        @TsidString Long propertyId,
        Long version,
        PublicationStatus publicationStatus,
        VerificationStatus verificationStatus,
        PublisherType publisherType,
        PropertyType propertyType,
        Instant createdAt
) {}
