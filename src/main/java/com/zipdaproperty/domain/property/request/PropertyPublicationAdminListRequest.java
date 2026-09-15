package com.zipdaproperty.domain.property.request;

import com.zipdaproperty.domain.property.constant.PropertyType;
import com.zipdaproperty.domain.property.constant.PublicationStatus;
import com.zipdaproperty.domain.property.constant.PublisherType;
import com.zipdaproperty.domain.property.constant.VerificationStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record PropertyPublicationAdminListRequest(
        PublicationStatus status,
        VerificationStatus verificationStatus,
        PublisherType publisherType,
        PropertyType propertyType,
        @Size(min = 1, max = 500) String cursor,
        @Min(1) @Max(50) Integer size
) {
    public PropertyPublicationAdminListRequest {
        if (status == null) status = PublicationStatus.IN_REVIEW;
        if (size == null) size = 20;
    }
}
