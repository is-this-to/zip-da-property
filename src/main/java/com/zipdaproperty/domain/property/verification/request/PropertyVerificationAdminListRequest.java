package com.zipdaproperty.domain.property.verification.request;

import com.zipdaproperty.domain.property.verification.constant.PropertyVerificationStatus;
import com.zipdaproperty.domain.property.verification.constant.PropertyVerificationType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record PropertyVerificationAdminListRequest(
        PropertyVerificationStatus status,
        PropertyVerificationType verificationType,
        @Size(min = 1, max = 500) String cursor,
        @Min(1) @Max(50) Integer size
) {
    public PropertyVerificationAdminListRequest {
        if (status == null) status = PropertyVerificationStatus.IN_REVIEW;
        if (size == null) size = 20;
    }
}
